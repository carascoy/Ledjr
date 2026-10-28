package com.Ledjr.service;

import static com.Ledjr.Fakes.assertMoney;
import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

/** budgets per category per month: spent, saved, the "you can budget" estimate and suggestions */
class BudgetServiceTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private final Category groceries = category(1, "Groceries", Category.Kind.expense);
    private final Category dining = category(2, "Dining Out", Category.Kind.expense);
    private final Category rent = category(3, "Rent", Category.Kind.expense);
    private final Category subscriptions = category(4, "Subscriptions", Category.Kind.expense);
    private final Category shopping = category(5, "Shopping", Category.Kind.expense);
    private final Category paycheck = category(8, "Paycheck", Category.Kind.income);

    private List<Budget> budgets;
    private List<Transaction> transactions;
    private List<RecurringTemplate> recurring;
    private List<Debt> debts;
    private List<Category> categories;

    @BeforeEach
    void setUp(){
        budgets = new ArrayList<>(List.of(
            budget(groceries, "2026-08-01", "400"), budget(dining, "2026-08-01", "100"),   // aug: $150 left over
            budget(groceries, "2026-09-01", "400"), budget(dining, "2026-09-01", "100"),   // sep: $50 over
            budget(groceries, "2026-10-01", "400"), budget(rent, "2026-10-01", "1200")));  // oct = this month
        transactions = new ArrayList<>(List.of(
            txn("expense", "300", "2026-08-10", groceries), txn("expense", "50", "2026-08-20", dining),
            txn("expense", "999", "2026-08-21", rent),
            txn("expense", "500", "2026-09-05", groceries), txn("expense", "50", "2026-09-06", dining),
            txn("expense", "1200", "2026-09-01", rent),
            txn("expense", "120.50", "2026-10-03", groceries), txn("expense", "45", "2026-10-04", dining),
            txn("expense", "1200", "2026-10-01", rent), txn("expense", "20", "2026-10-09", null),
            txn("income", "3300", "2026-10-02", paycheck), txn("payment", "313", "2026-10-05", null)));
        recurring = new ArrayList<>(List.of(
            recurring("Paycheck", RecurringTemplate.Type.income, "1650", RecurringTemplate.Frequency.biweekly, "2026-10-02", paycheck),
            recurring("Rent", RecurringTemplate.Type.expense, "1200", RecurringTemplate.Frequency.monthly, "2026-01-01", rent),
            recurring("Spotify", RecurringTemplate.Type.expense, "11.99", RecurringTemplate.Frequency.monthly, "2026-03-15", subscriptions),
            recurring("Phone", RecurringTemplate.Type.expense, "45", RecurringTemplate.Frequency.monthly, "2026-01-05", null)));
        debts = new ArrayList<>(List.of(debt("313", "11687"), debt("60", "2700"), debt("100", "0")));
        categories = List.of(dining, groceries, paycheck, rent, shopping, subscriptions);   // A to Z like the real query
    }

    private BudgetService service(){
        BudgetRepository budgetRepo = fake(BudgetRepository.class, Map.of(
            "findByUserIdAndMonth", a -> budgets.stream().filter(b -> b.getMonth().equals(a[1])).toList(),
            "findByUserIdAndMonthBefore", a -> budgets.stream().filter(b -> b.getMonth().isBefore((LocalDate) a[1])).toList(),
            "existsByUserIdAndMonth", a -> budgets.stream().anyMatch(b -> b.getMonth().equals(a[1])),
            "findByCategoryIdAndMonth", a -> budgets.stream()
                .filter(b -> b.getCategory().getId().equals(a[0]) && b.getMonth().equals(a[1])).findFirst(),
            "save", a -> { Budget b = (Budget) a[0]; if (!budgets.contains(b)) budgets.add(b); return b; },
            "delete", a -> { budgets.remove(a[0]); return null; }));
        TransactionRepository txnRepo = fake(TransactionRepository.class, Map.of(
            "findByUserIdAndTxnDateBetween", a -> transactions.stream()
                .filter(t -> !t.getTxnDate().isBefore((LocalDate) a[1]) && !t.getTxnDate().isAfter((LocalDate) a[2])).toList()));
        CategoryRepository categoryRepo = fake(CategoryRepository.class, Map.of("findByUserIdOrderByNameAsc", a -> categories));
        RecurringTemplateRepository recurringRepo = fake(RecurringTemplateRepository.class, Map.of("findByUserId", a -> recurring));
        DebtRepository debtRepo = fake(DebtRepository.class, Map.of("findByUserId", a -> debts));
        return new BudgetService(budgetRepo, txnRepo, categoryRepo, recurringRepo, debtRepo);
    }

    @Test
    @DisplayName("spent only counts expenses, budgeted categories come first")
    void monthTotals(){
        BudgetResponse oct = service().forMonth(7L, OCT, OCT);
        assertEquals(List.of("Groceries", "Rent", "Dining Out", "Shopping", "Subscriptions"),
            oct.lines().stream().map(BudgetLine::name).toList());
        assertMoney("1600", oct.totalBudgeted());
        assertMoney("1320.50", oct.totalSpent());
        // dining $45 (no budget this month) + $20 uncategorized. the $313 debt payment isn't spending
        assertMoney("65", oct.unbudgetedSpent());
        assertMoney("3300", oct.income());
    }

    @Test
    @DisplayName("savings: a month under budget adds what's left, a month over adds $0")
    void savedTotal(){
        // aug: 400-300 + 100-50 = 150 left. sep: 400-500 + 100-50 = -50, adds 0. oct isn't over yet
        assertMoney("150", service().savedTotal(7L, OCT));
    }

    @Test
    @DisplayName("you can budget = paychecks expected this month - debt minimums - bills with no category")
    void estimate(){
        BudgetResponse oct = service().forMonth(7L, OCT, OCT);
        assertEquals(3, oct.paycheckCount());              // oct 2, 16, 30
        assertMoney("4950", oct.expectedIncome());          // beats the 3300 logged so far
        assertEquals("recurring", oct.incomeBasis());
        assertMoney("373", oct.debtMinimums());             // the paid off loan doesn't count
        assertMoney("45", oct.otherBills());                // the phone bill has no category
        assertMoney("4532", oct.canBudget());
    }

    @Test
    @DisplayName("logged more than expected (a bonus): the estimate uses what actually came in")
    void bonusBeatsTheEstimate(){
        transactions.add(txn("income", "2000", "2026-10-20", paycheck));
        BudgetResponse oct = service().forMonth(7L, OCT, OCT);
        assertMoney("5300", oct.expectedIncome());
        assertEquals("logged", oct.incomeBasis());
    }

    @Test
    @DisplayName("no paycheck set up: falls back to last month's income")
    void fallsBackToLastMonth(){
        recurring.clear();
        transactions.clear();
        transactions.add(txn("income", "2800", "2026-09-15", paycheck));
        BudgetResponse oct = service().forMonth(7L, OCT, OCT);
        assertEquals("last-month", oct.incomeBasis());
        assertMoney("2800", oct.expectedIncome());
    }

    @Test
    @DisplayName("brand new account: nothing to estimate, no suggestions, fill does nothing")
    void brandNewAccount(){
        budgets.clear();
        transactions.clear();
        recurring.clear();
        debts.clear();
        BudgetService service = service();
        BudgetResponse oct = service.forMonth(7L, OCT, OCT);
        assertEquals("none", oct.incomeBasis());
        assertMoney("0", oct.canBudget());
        assertTrue(oct.lines().stream().allMatch(l -> l.suggested() == null));
        assertEquals(0, service.fillSuggestions(new AppUser(), OCT, OCT));
    }

    @Test
    @DisplayName("suggestions: the recurring bill if it's bigger, otherwise the 3 month average rounded up")
    void suggestions(){
        BudgetResponse nov = service().forMonth(7L, NOV, OCT);
        assertMoney("307", line(nov, "Groceries").suggested());        // (300 + 500 + 120.50) / 3 = 306.83
        assertEquals("average", line(nov, "Groceries").suggestionReason());
        assertMoney("49", line(nov, "Dining Out").suggested());        // 145 / 3 = 48.33
        assertMoney("1200", line(nov, "Rent").suggested());            // the bill beats the 1133 average
        assertEquals("bill", line(nov, "Rent").suggestionReason());
        assertMoney("11.99", line(nov, "Subscriptions").suggested());
        assertNull(line(nov, "Shopping").suggested());
    }

    @Test
    @DisplayName("a new account isn't averaged down by months it didn't exist yet")
    void averageSkipsEmptyMonths(){
        transactions.clear();
        transactions.add(txn("expense", "90", "2026-09-16", groceries));
        assertMoney("90", line(service().forMonth(7L, OCT, OCT), "Groceries").suggested());
    }

    @Test
    @DisplayName("fill budgets every suggested category once, running it again does nothing")
    void fillSuggestions(){
        BudgetService service = service();
        assertEquals(4, service.fillSuggestions(new AppUser(), NOV, OCT));
        assertMoney("1567.99", service.forMonth(7L, NOV, OCT).totalBudgeted());
        assertEquals(0, service.fillSuggestions(new AppUser(), NOV, OCT));
    }

    @Test
    @DisplayName("copy last month, then change one, then remove one")
    void copySetRemove(){
        BudgetService service = service();
        assertTrue(service.forMonth(7L, NOV, OCT).canCopyLastMonth());
        assertEquals(2, service.copyFromPreviousMonth(new AppUser(), NOV));
        assertFalse(service.forMonth(7L, NOV, OCT).canCopyLastMonth());

        service.set(new AppUser(), groceries, NOV, new BigDecimal("450.00"));
        assertMoney("1650", service.forMonth(7L, NOV, OCT).totalBudgeted());
        service.set(new AppUser(), rent, NOV, null);
        assertMoney("450", service.forMonth(7L, NOV, OCT).totalBudgeted());
    }

    // ---- building test data ----

    private static BudgetLine line(BudgetResponse r, String name){
        return r.lines().stream().filter(l -> l.name().equals(name)).findFirst().orElseThrow();
    }

    private static Category category(long id, String name, Category.Kind kind){
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setKind(kind);
        c.setColorSlot((int) id % 8);
        return c;
    }

    private static Budget budget(Category c, String month, String amount){
        Budget b = new Budget();
        b.setCategory(c);
        b.setMonth(LocalDate.parse(month));
        b.setAmount(new BigDecimal(amount));
        return b;
    }

    private static Transaction txn(String type, String amount, String date, Category c){
        Transaction t = new Transaction();
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setTxnDate(LocalDate.parse(date));
        t.setCategory(c);
        return t;
    }

    private static RecurringTemplate recurring(String name, RecurringTemplate.Type type, String amount,
                                               RecurringTemplate.Frequency frequency, String start, Category c){
        RecurringTemplate t = new RecurringTemplate();
        t.setName(name);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setFrequency(frequency);
        t.setStartDate(LocalDate.parse(start));
        t.setCategory(c);
        return t;
    }

    private static Debt debt(String minimum, String balance){
        Debt d = new Debt();
        d.setMinPayment(new BigDecimal(minimum));
        d.setBalance(new BigDecimal(balance));
        return d;
    }
}
