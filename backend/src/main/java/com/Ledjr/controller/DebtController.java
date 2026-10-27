package com.Ledjr.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.CreateDebtRequest;
import com.Ledjr.dto.DebtResponse;
import com.Ledjr.dto.DebtSummary;
import com.Ledjr.dto.SetPlanRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Debt;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.DebtService;

@RestController
@RequestMapping("/debts")
public class DebtController {

    private static final int MAX_NAME_LENGTH = 60;
    // NUMERIC(10,2) maxes out at 99,999,999.99
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_APR = new BigDecimal("100");
    private static final int MAX_PLAN_YEARS = 30;

    private final DebtService debtService;
    private final AppUserService appUserService;

    public DebtController(DebtService debtService, AppUserService appUserService){
        this.debtService = debtService;
        this.appUserService = appUserService;
    }

    /** GET /debts - your debts, biggest balance first */
    @GetMapping
    public ResponseEntity<List<DebtResponse>> list(@AuthenticationPrincipal Long userId){
        List<DebtResponse> body = debtService.findByUserIdLargestFirst(userId)
            .stream()
            .map(DebtController::toResponse)
            .toList();
        return ResponseEntity.ok(body);
    }

    /** GET /debts/summary - totals + percents for the dashboard pill */
    @GetMapping("/summary")
    public ResponseEntity<DebtSummary> summary(@AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(debtService.getSummary(userId));
    }

    /** POST /debts - add a debt */
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal Long userId, @RequestBody CreateDebtRequest request){

        String name = request.creditorName() == null ? "" : request.creditorName().trim();
        if (name.isEmpty()) {
            return badRequest("Give the debt a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return badRequest("Name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }

        // loan or card, defaults to loan so old requests still work
        Debt.Kind kind;
        if (request.kind() == null || "loan".equals(request.kind())) {
            kind = Debt.Kind.loan;
        } else if ("card".equals(request.kind())) {
            kind = Debt.Kind.card;
        } else {
            return badRequest("Kind must be loan or card.");
        }

        BigDecimal balance = request.balance();
        if (balance == null || balance.signum() < 0 || balance.compareTo(MAX_AMOUNT) > 0) {
            return badRequest("Balance must be between $0 and $99,999,999.99.");
        }

        BigDecimal original = null;
        BigDecimal limit = null;
        if (kind == Debt.Kind.loan) {
            // starting amount is optional, defaults to the balance now and can't be less than it
            original = request.originalBalance() == null ? balance : request.originalBalance();
            if (original.compareTo(balance) < 0 || original.compareTo(MAX_AMOUNT) > 0 || original.signum() <= 0) {
                return badRequest("Original amount must be at least the current balance (and more than $0).");
            }
        } else {
            // cards need a limit (utilization = balance / limit). going over the limit is allowed, it happens
            limit = request.creditLimit();
            if (limit == null || limit.signum() <= 0 || limit.compareTo(MAX_AMOUNT) > 0) {
                return badRequest("A card needs its credit limit (more than $0).");
            }
            original = balance; // cards don't use this but the column is NOT NULL
        }

        if (request.apr() != null && (request.apr().signum() < 0 || request.apr().compareTo(MAX_APR) > 0)) {
            return badRequest("APR must be between 0 and 100.");
        }
        if (request.minPayment() != null && (request.minPayment().signum() < 0 || request.minPayment().compareTo(MAX_AMOUNT) > 0)) {
            return badRequest("Minimum payment can't be negative.");
        }
        if (request.dueDate() != null && (request.dueDate() < 1 || request.dueDate() > 31)) {
            return badRequest("Due day must be between 1 and 31.");
        }

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        Debt saved = debtService.newDebt(
            user.get(), kind, name, balance, original, limit, request.apr(), request.minPayment(), request.dueDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /** PUT /debts/{id}/plan - set or clear (null) the payoff target month */
    @PutMapping("/{id}/plan")
    public ResponseEntity<?> setPlan(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody SetPlanRequest request){
        Optional<Debt> found = debtService.findById(id);
        // same 404 if it doesn't exist or isn't theirs
        if (found.isEmpty() || !found.get().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Debt not found.");
        }

        LocalDate target = request.payoffTarget();
        if (target != null) {
            // always store the 1st so "months left" math is clean
            target = target.withDayOfMonth(1);
            LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
            if (!target.isAfter(thisMonth)) {
                return badRequest("Pick a month after this one.");
            }
            if (target.isAfter(thisMonth.plusYears(MAX_PLAN_YEARS))) {
                return badRequest("Plans can go up to " + MAX_PLAN_YEARS + " years out.");
            }
        }

        return ResponseEntity.ok(toResponse(debtService.setPayoffTarget(found.get(), target)));
    }

    /** DELETE /debts/{id} - payments to it go too, card purchases stay as normal spending */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id){
        Optional<Debt> found = debtService.findById(id);
        // same 404 if it doesn't exist or isn't theirs
        if (found.isEmpty() || !found.get().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Debt not found.");
        }
        debtService.delete(found.get());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<String> badRequest(String message){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    /** entity -> DTO so entities (and the password hash) never leave the server */
    static DebtResponse toResponse(Debt d){
        return new DebtResponse(d.getId(), d.getKind().name(), d.getCreditorName(), d.getBalance(),
            d.getOriginalBalance(), d.getCreditLimit(), d.getApr(), d.getMinPayment(), d.getDueDate(), d.getPayoffTarget());
    }
}
