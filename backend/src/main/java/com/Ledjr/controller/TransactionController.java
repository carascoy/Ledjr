package com.Ledjr.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** POST /transactions - add one */
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal Long userId, @RequestBody CreateTransactionRequest request){
        return save(userId, request, null);
    }

    /** PUT /transactions/{id} - replace it. same body as POST, the whole thing gets resent */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody CreateTransactionRequest request){
        Optional<Transaction> existing = transactionService.findById(id);
        // same 404 if it doesn't exist or isn't theirs
        if (existing.isEmpty() || !existing.get().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found.");
        }
        return save(userId, request, existing.get());
    }

    /** DELETE /transactions/{id} - puts any debt balance back first */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id){
        Optional<Transaction> existing = transactionService.findById(id);
        if (existing.isEmpty() || !existing.get().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found.");
        }
        try {
            transactionService.delete(existing.get());
        } catch (TransactionService.BalanceWouldGoNegativeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    /** create and edit share all the same checks. existing = null means create */
    private ResponseEntity<?> save(Long userId, CreateTransactionRequest request, Transaction existing){

        if (request.amount() == null || request.amount().signum() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount must be greater than 0.");
        }
        String type = request.type();
        boolean isPayment = "payment".equals(type);
        if (!"income".equals(type) && !"expense".equals(type) && !isPayment) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Type must be income, expense or payment.");
        }

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // category is optional but has to exist, be theirs, and match the type.
        // payments don't get one, they're not income or spending, just money going to a debt
        Category category = null;
        if (isPayment && request.categoryId() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payments don't take a category.");
        }
        if (request.categoryId() != null) {
            Optional<Category> found = categoryService.findById(request.categoryId());
            // same 404 if it doesn't exist or isn't theirs, so you can't fish for other people's ids
            if (found.isEmpty() || !found.get().getUser().getId().equals(user.get().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found.");
            }
            category = found.get();
            if (!category.getKind().name().equals(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("\"" + category.getName() + "\" is an " + category.getKind().name() + " category.");
            }
        }

        // debt: required for payments, optional for expenses (the card), never on income
        Debt debt = null;
        if (isPayment && request.debtId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Pick which debt this payment went to.");
        }
        if (request.debtId() != null) {
            if ("income".equals(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Income can't be linked to a debt.");
            }
            Optional<Debt> found = debtService.findById(request.debtId());
            if (found.isEmpty() || !found.get().getUser().getId().equals(user.get().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Debt not found.");
            }
            debt = found.get();
            if ("expense".equals(type) && debt.getKind() != Debt.Kind.card) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Purchases can only go on a card, not a loan.");
            }
            // when editing, the old version's effect gets undone first, so count that in
            BigDecimal left = transactionService.balanceWithout(existing, debt);
            if (isPayment && request.amount().compareTo(left) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("That's more than the $" + left.toPlainString() + " left on " + debt.getCreditorName() + ".");
            }
        }

        LocalDate date = request.txnDate() != null ? request.txnDate() : LocalDate.now();

        if (existing == null) {
            Transaction saved = transactionService.record(user.get(), category, debt, type, request.amount(), date, request.description());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        }
        try {
            Transaction saved = transactionService.update(existing, category, debt, type, request.amount(), date, request.description());
            return ResponseEntity.ok(toResponse(saved));
        } catch (TransactionService.BalanceWouldGoNegativeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** GET /transactions?month=2026-10 - one month, newest first */
    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal Long userId, @RequestParam String month){
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);          // wants "2026-10"
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Month must look like 2026-10.");
        }

        List<TransactionResponse> body = transactionService.findForMonth(userId, ym)
            .stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(body);
    }

    /** entity -> DTO so entities (and the password hash) never leave the server */
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
