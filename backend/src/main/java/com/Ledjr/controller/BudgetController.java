package com.Ledjr.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.SetBudgetRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.BudgetService;
import com.Ledjr.service.CategoryService;

@RestController
@RequestMapping("/budget")
public class BudgetController {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private final BudgetService budgetService;
    private final AppUserService appUserService;
    private final CategoryService categoryService;

    public BudgetController(BudgetService budgetService, AppUserService appUserService, CategoryService categoryService){
        this.budgetService = budgetService;
        this.appUserService = appUserService;
        this.categoryService = categoryService;
    }

    /** GET /budget?month=2026-10 - every expense category with its budget + what's been spent */
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal Long userId, @RequestParam String month){
        YearMonth ym = parseMonth(month);
        if (ym == null) return badMonth();
        return ResponseEntity.ok(budgetService.forMonth(userId, ym, YearMonth.now()));
    }

    /** PUT /budget/{categoryId}?month=2026-10 {amount} - set it, or amount null/0 to remove it */
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> set(@AuthenticationPrincipal Long userId, @PathVariable Long categoryId,
                                 @RequestParam String month, @RequestBody SetBudgetRequest request){
        YearMonth ym = parseMonth(month);
        if (ym == null) return badMonth();

        Optional<Category> category = categoryService.findById(categoryId);
        if (category.isEmpty() || !category.get().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found.");
        }
        if (category.get().getKind() != Category.Kind.expense) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only expense categories get a budget.");
        }

        BigDecimal amount = request.amount();
        if (amount != null) {
            if (amount.signum() < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Budget can't be negative.");
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Budget can't be more than $1,000,000.");
            }
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        budgetService.set(user.get(), category.get(), ym, amount);
        // send the whole month back so the page doesn't need a 2nd request
        return ResponseEntity.ok(budgetService.forMonth(userId, ym, YearMonth.now()));
    }

    /** POST /budget/copy?month=2026-11 - fill an empty month with last month's amounts */
    @PostMapping("/copy")
    public ResponseEntity<?> copy(@AuthenticationPrincipal Long userId, @RequestParam String month){
        YearMonth ym = parseMonth(month);
        if (ym == null) return badMonth();

        if (budgetService.hasBudget(userId, ym)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This month already has a budget.");
        }
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        if (budgetService.copyFromPreviousMonth(user.get(), ym) == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Last month doesn't have a budget to copy.");
        }
        return ResponseEntity.ok(budgetService.forMonth(userId, ym, YearMonth.now()));
    }

    /** POST /budget/fill?month=2026-11 - budget every category that has a suggestion and no budget yet */
    @PostMapping("/fill")
    public ResponseEntity<?> fill(@AuthenticationPrincipal Long userId, @RequestParam String month){
        YearMonth ym = parseMonth(month);
        if (ym == null) return badMonth();

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        if (budgetService.fillSuggestions(user.get(), ym, YearMonth.now()) == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nothing to suggest yet. Add your bills on Recurring or log some spending first.");
        }
        return ResponseEntity.ok(budgetService.forMonth(userId, ym, YearMonth.now()));
    }

    private static YearMonth parseMonth(String month){
        try {
            return YearMonth.parse(month);   // wants "2026-10"
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static ResponseEntity<String> badMonth(){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Month must look like 2026-10.");
    }
}
