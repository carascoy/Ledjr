package com.Ledjr.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Ledjr.dto.BudgetLine;
import com.Ledjr.dto.BudgetResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Budget;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.BudgetRepository;
import com.Ledjr.repository.CategoryRepository;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;

/**
 * budgets are per category per month. spent, saved, the estimate and suggestions aren't stored,
 * they get worked out from transactions/recurring/debts every time so they can't drift
 */
@Service
public class BudgetService {

    /** how many past months the "you usually spend" average looks at */
    private static final int AVERAGE_MONTHS = 3;

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringTemplateRepository recurringRepository;
    private final DebtRepository debtRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository, RecurringTemplateRepository recurringRepository,
                         DebtRepository debtRepository){
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.recurringRepository = recurringRepository;
        this.debtRepository = debtRepository;
    }

    /** a suggested amount + why. reason is "bill" or "average" */
    record Suggestion(BigDecimal amount, String reason) {}

    /** everything the budget page needs for one month. thisMonth is passed in so tests can fake "now" */
    public BudgetResponse forMonth(Long userId, YearMonth month, YearMonth thisMonth){
        Map<Long, BigDecimal> budgeted = new HashMap<>();
        for (Budget b : budgetRepository.findByUserIdAndMonth(userId, month.atDay(1))) {
            budgeted.put(b.getCategory().getId(), b.getAmount());
        }

        // add up the month's expenses per category (null key = uncategorized) + income
        Map<Long, BigDecimal> spent = new HashMap<>();
        BigDecimal allExpenses = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        for (Transaction t : transactionsIn(userId, month)) {
            if ("expense".equals(t.getType())) {
                Long key = t.getCategory() == null ? null : t.getCategory().getId();
                spent.merge(key, t.getAmount(), BigDecimal::add);
                allExpenses = allExpenses.add(t.getAmount());
            } else if ("income".equals(t.getType())) {
                income = income.add(t.getAmount());
            }
        }

        // recurring stuff landing in this month: paychecks, bills per category, bills with no category
        List<RecurringTemplate> recurring = recurringRepository.findByUserId(userId);
        BigDecimal recurringIncome = BigDecimal.ZERO;
        int paychecks = 0;
        BigDecimal otherBills = BigDecimal.ZERO;
        Map<Long, BigDecimal> billsByCategory = new HashMap<>();
        for (RecurringTemplate t : recurring) {
            int times = RecurringTemplateService.occurrences(t, month.atDay(1), month.atEndOfMonth()).size();
            if (times == 0) continue;
            BigDecimal total = t.getAmount().multiply(BigDecimal.valueOf(times));
            if (t.getType() == RecurringTemplate.Type.income) {
                recurringIncome = recurringIncome.add(total);
                paychecks += times;
            } else if (t.getCategory() == null) {
                otherBills = otherBills.add(total);
            } else {
                billsByCategory.merge(t.getCategory().getId(), total, BigDecimal::add);
            }
        }

        // what the estimate starts from: expected paychecks, or what's been logged if that's more,
        // or last month's income if there's nothing else to go on
        BigDecimal expectedIncome;
        String basis;
        if (recurringIncome.signum() > 0 || income.signum() > 0) {
            boolean useRecurring = recurringIncome.compareTo(income) >= 0;
            expectedIncome = useRecurring ? recurringIncome : income;
            basis = useRecurring ? "recurring" : "logged";
        } else {
            BigDecimal lastMonth = incomeIn(userId, month.minusMonths(1));
            expectedIncome = lastMonth;
            basis = lastMonth.signum() > 0 ? "last-month" : "none";
        }

        BigDecimal debtMinimums = BigDecimal.ZERO;
        for (Debt d : debtRepository.findByUserId(userId)) {
            if (d.getMinPayment() != null && d.getBalance() != null && d.getBalance().signum() > 0) {
                debtMinimums = debtMinimums.add(d.getMinPayment());
            }
        }

        Map<Long, Suggestion> suggestions = suggestions(userId, month, billsByCategory);

        // budgeted categories first, then the rest. both A to Z since the repo already sorts
        List<BudgetLine> withBudget = new ArrayList<>();
        List<BudgetLine> without = new ArrayList<>();
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (Category c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
            if (c.getKind() != Category.Kind.expense) continue;
            BigDecimal amount = budgeted.get(c.getId());
            BigDecimal catSpent = spent.getOrDefault(c.getId(), BigDecimal.ZERO);
            Suggestion s = suggestions.get(c.getId());
            BudgetLine line = new BudgetLine(c.getId(), c.getName(), c.getColorSlot(), amount, catSpent,
                s == null ? null : s.amount(), s == null ? null : s.reason());
            if (amount != null) {
                withBudget.add(line);
                totalBudgeted = totalBudgeted.add(amount);
                totalSpent = totalSpent.add(catSpent);
            } else {
                without.add(line);
            }
        }
        withBudget.addAll(without);

        boolean canCopy = budgeted.isEmpty()
            && budgetRepository.existsByUserIdAndMonth(userId, month.minusMonths(1).atDay(1));

        return new BudgetResponse(
            month.toString(), withBudget, totalBudgeted, totalSpent,
            allExpenses.subtract(totalSpent), income, savedTotal(userId, thisMonth), canCopy,
            expectedIncome, basis, paychecks, debtMinimums, otherBills,
            expectedIncome.subtract(debtMinimums).subtract(otherBills));
    }

    /**
     * per category: the recurring bills in it this month, or what you usually spend there
     * (average of the last 3 months, rounded up to a whole dollar), whichever is bigger.
     * months with nothing logged at all don't count, so a brand new account isn't averaged down
     */
    Map<Long, Suggestion> suggestions(Long userId, YearMonth month, Map<Long, BigDecimal> billsByCategory){
        YearMonth first = month.minusMonths(AVERAGE_MONTHS);
        YearMonth last = month.minusMonths(1);

        Map<Long, BigDecimal> spentInWindow = new HashMap<>();
        Set<YearMonth> activeMonths = new HashSet<>();
        for (Transaction t : transactionRepository.findByUserIdAndTxnDateBetween(userId, first.atDay(1), last.atEndOfMonth())) {
            activeMonths.add(YearMonth.from(t.getTxnDate()));
            if ("expense".equals(t.getType()) && t.getCategory() != null) {
                spentInWindow.merge(t.getCategory().getId(), t.getAmount(), BigDecimal::add);
            }
        }

        Map<Long, Suggestion> out = new HashMap<>();
        Set<Long> categoryIds = new HashSet<>(billsByCategory.keySet());
        categoryIds.addAll(spentInWindow.keySet());
        for (Long id : categoryIds) {
            BigDecimal bill = billsByCategory.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal average = activeMonths.isEmpty() ? BigDecimal.ZERO
                : spentInWindow.getOrDefault(id, BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(activeMonths.size()), 0, RoundingMode.CEILING);
            if (bill.signum() == 0 && average.signum() == 0) continue;
            out.put(id, bill.compareTo(average) >= 0
                ? new Suggestion(bill, "bill")
                : new Suggestion(average.setScale(2), "average"));
        }
        return out;
    }

    /**
     * savings = for every finished month, budget minus what you spent in those categories.
     * a month you went over adds 0, it doesn't eat into earlier savings. every month starts fresh
     */
    public BigDecimal savedTotal(Long userId, YearMonth thisMonth){
        List<Budget> past = budgetRepository.findByUserIdAndMonthBefore(userId, thisMonth.atDay(1));
        if (past.isEmpty()) return BigDecimal.ZERO;

        LocalDate earliest = past.stream().map(Budget::getMonth).min(LocalDate::compareTo).get();
        LocalDate lastDay = thisMonth.minusMonths(1).atEndOfMonth();

        // spent per (month, category)
        Map<String, BigDecimal> spent = new HashMap<>();
        for (Transaction t : transactionRepository.findByUserIdAndTxnDateBetween(userId, earliest, lastDay)) {
            if (!"expense".equals(t.getType()) || t.getCategory() == null) continue;
            spent.merge(YearMonth.from(t.getTxnDate()) + ":" + t.getCategory().getId(), t.getAmount(), BigDecimal::add);
        }

        // what was left in each month across all its budgeted categories
        Map<LocalDate, BigDecimal> leftPerMonth = new HashMap<>();
        for (Budget b : past) {
            BigDecimal catSpent = spent.getOrDefault(YearMonth.from(b.getMonth()) + ":" + b.getCategory().getId(), BigDecimal.ZERO);
            leftPerMonth.merge(b.getMonth(), b.getAmount().subtract(catSpent), BigDecimal::add);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal left : leftPerMonth.values()) {
            if (left.signum() > 0) total = total.add(left);
        }
        return total;
    }

    /** set one category's budget for a month. null or 0 removes it */
    @Transactional
    public void set(AppUser user, Category category, YearMonth month, BigDecimal amount){
        Optional<Budget> existing = budgetRepository.findByCategoryIdAndMonth(category.getId(), month.atDay(1));
        if (amount == null || amount.signum() <= 0) {
            existing.ifPresent(budgetRepository::delete);
            return;
        }
        Budget b = existing.orElseGet(Budget::new);
        b.setUser(user);
        b.setCategory(category);
        b.setMonth(month.atDay(1));
        b.setAmount(amount);
        budgetRepository.save(b);
    }

    /** start a month off with last month's amounts. returns how many got copied */
    @Transactional
    public int copyFromPreviousMonth(AppUser user, YearMonth month){
        List<Budget> previous = budgetRepository.findByUserIdAndMonth(user.getId(), month.minusMonths(1).atDay(1));
        for (Budget old : previous) {
            Budget b = new Budget();
            b.setUser(user);
            b.setCategory(old.getCategory());
            b.setMonth(month.atDay(1));
            b.setAmount(old.getAmount());
            budgetRepository.save(b);
        }
        return previous.size();
    }

    /** budget every category that has a suggestion but no budget yet. returns how many got set */
    @Transactional
    public int fillSuggestions(AppUser user, YearMonth month, YearMonth thisMonth){
        BudgetResponse current = forMonth(user.getId(), month, thisMonth);
        Map<Long, Category> categories = new HashMap<>();
        for (Category c : categoryRepository.findByUserIdOrderByNameAsc(user.getId())) {
            categories.put(c.getId(), c);
        }
        int filled = 0;
        for (BudgetLine line : current.lines()) {
            if (line.budgeted() != null || line.suggested() == null) continue;
            set(user, categories.get(line.categoryId()), month, line.suggested());
            filled++;
        }
        return filled;
    }

    public boolean hasBudget(Long userId, YearMonth month){
        return budgetRepository.existsByUserIdAndMonth(userId, month.atDay(1));
    }

    private List<Transaction> transactionsIn(Long userId, YearMonth month){
        return transactionRepository.findByUserIdAndTxnDateBetween(userId, month.atDay(1), month.atEndOfMonth());
    }

    private BigDecimal incomeIn(Long userId, YearMonth month){
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactionsIn(userId, month)) {
            if ("income".equals(t.getType())) total = total.add(t.getAmount());
        }
        return total;
    }
}
