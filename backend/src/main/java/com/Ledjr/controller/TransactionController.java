package com.Ledjr.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.CreateTransactionRequest;
import com.Ledjr.dto.TransactionResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.Transaction;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.CategoryService;
import com.Ledjr.service.DebtService;
import com.Ledjr.service.TransactionService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AppUserService appUserService;
    private final CategoryService categoryService;
    private final DebtService debtService;

    public TransactionController(TransactionService transactionService, AppUserService appUserService,
                                 CategoryService categoryService, DebtService debtService){
        this.transactionService = transactionService;
        this.appUserService = appUserService;
        this.categoryService = categoryService;
        this.debtService = debtService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTransactionRequest request){

        if (request.amount() == null || request.amount().signum() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount must be greater than 0.");
        }
        String type = request.type();
        boolean isPayment = "payment".equals(type);
        if (!"income".equals(type) && !"expense".equals(type) && !isPayment) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Type must be income, expense or payment.");
        }

        Optional<AppUser> user = appUserService.findById(request.userId());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // Optional category: it must exist, belong to this user, and match the transaction type.
        // Payments don't get one: they aren't income or spending, just money moving to a debt.
        Category category = null;
        if (isPayment && request.categoryId() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payments don't take a category.");
        }
        if (request.categoryId() != null) {
            Optional<Category> found = categoryService.findById(request.categoryId());
            // Same 404 whether it doesn't exist or belongs to someone else, so other users' ids aren't revealed.
            if (found.isEmpty() || !found.get().getUser().getId().equals(user.get().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found.");
            }
            category = found.get();
            if (!category.getKind().name().equals(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("\"" + category.getName() + "\" is an " + category.getKind().name() + " category.");
            }
        }

        // Optional debt: required for a payment, optional for an expense (the card it was put on), never for income.
        Debt debt = null;
        if (isPayment && request.debtId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Pick which debt this payment went to.");
        }
        if (request.debtId() != null) {
            if ("income".equals(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Income can't be linked to a debt.");
            }
            Optional<Debt> found = debtService.findById(request.debtId());
            // Same 404 for "doesn't exist" and "someone else's", like categories.
            if (found.isEmpty() || !found.get().getUser().getId().equals(user.get().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Debt not found.");
            }
            debt = found.get();
            if ("expense".equals(type) && debt.getKind() != Debt.Kind.card) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Purchases can only go on a card, not a loan.");
            }
            if (isPayment && request.amount().compareTo(debt.getBalance()) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("That's more than the $" + debt.getBalance().toPlainString() + " left on " + debt.getCreditorName() + ".");
            }
        }

        LocalDate date = request.txnDate() != null ? request.txnDate() : LocalDate.now();

        // record() saves the transaction and updates the debt's balance together (one database transaction).
        Transaction saved = transactionService.record(user.get(), category, debt, type, request.amount(), date, request.description());

        TransactionResponse response = toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /transactions?userId=1&month=2026-10 — one month's transactions, newest first. */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam Long userId, @RequestParam String month){
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);          // expects "2026-10"
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Month must look like 2026-10.");
        }

        List<TransactionResponse> body = transactionService.findForMonth(userId, ym)
            .stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(body);
    }

    /** Entity -> DTO, so entities (and the user's password hash) never leave the server. */
    private TransactionResponse toResponse(Transaction t){
        Category c = t.getCategory();
        Debt d = t.getDebt();
        return new TransactionResponse(
            t.getId(), t.getType(), t.getAmount(), t.getTxnDate(), t.getDescription(),
            c == null ? null : c.getId(),
            c == null ? null : c.getName(),
            c == null ? null : c.getColorSlot(),
            d == null ? null : d.getId(),
            d == null ? null : d.getCreditorName());
    }
}
