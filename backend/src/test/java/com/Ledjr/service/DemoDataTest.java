package com.Ledjr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.Transaction;

class DemoDataTest {

    private static final List<String> TODAYS = List.of(
        "2026-11-11", "2026-11-01", "2026-01-03", "2028-02-29", "2026-12-31");

    private static DemoData.Plan planFor(String today){
        AppUser user = new AppUser();
        Map<String, Category> categories = new HashMap<>();
        for (String name : List.of("Groceries", "Rent", "Dining Out", "Transportation", "Utilities",
                                   "Shopping", "Entertainment", "Paycheck")) {
            Category c = new Category();
            c.setName(name);
            c.setUser(user);
            categories.put(name, c);
        }
        return DemoData.build(user, categories, LocalDate.parse(today), 42);
    }

    @Test
    @DisplayName("nothing is dated in the future or before the history starts")
    void datesStayInRange(){
        for (String day : TODAYS) {
            LocalDate today = LocalDate.parse(day);
            for (Transaction t : planFor(day).transactions()) {
                assertFalse(t.getTxnDate().isAfter(today), day + ": " + t.getDescription() + " on " + t.getTxnDate());
                assertFalse(t.getTxnDate().isBefore(DemoData.historyStart(today)), day + ": " + t.getDescription());
            }
        }
    }

    @Test
    @DisplayName("every bill and paycheck due so far is paid, so the calendar shows nothing late")
    void everyPastDueDateIsPaid(){
        for (String day : TODAYS) {
            LocalDate today = LocalDate.parse(day);
            DemoData.Plan plan = planFor(day);
            for (RecurringTemplate r : plan.recurring()) {
                for (LocalDate due : RecurringTemplateService.occurrences(r, r.getCreatedAt().toLocalDate(), today)) {
                    boolean paid = plan.transactions().stream()
                        .anyMatch(t -> t.getRecurringTemplate() == r && due.equals(t.getDueDate()));
                    assertTrue(paid, day + ": " + r.getName() + " due " + due + " isn't paid");
                }
            }
        }
    }

    @Test
    @DisplayName("payments are only marked against real due dates")
    void paymentsLandOnRealDueDates(){
        for (String day : TODAYS) {
            for (Transaction t : planFor(day).transactions()) {
                if (t.getRecurringTemplate() == null) continue;
                List<LocalDate> due = RecurringTemplateService.occurrences(t.getRecurringTemplate(), t.getDueDate(), t.getDueDate());
                assertEquals(List.of(t.getDueDate()), due, day + ": " + t.getDescription());
            }
        }
    }

    @Test
    @DisplayName("the card's balance matches its purchases and payments, and stays under the limit")
    void cardBalanceAddsUp(){
        for (String day : TODAYS) {
            DemoData.Plan plan = planFor(day);
            Debt card = plan.debts().stream().filter(d -> d.getKind() == Debt.Kind.card).findFirst().orElseThrow();
            BigDecimal expected = DemoData.CARD_AT_START;
            for (Transaction t : plan.transactions()) {
                if (t.getDebt() != card) continue;
                expected = "payment".equals(t.getType()) ? expected.subtract(t.getAmount()) : expected.add(t.getAmount());
            }
            assertEquals(0, expected.compareTo(card.getBalance()), day);
            assertTrue(card.getBalance().signum() > 0, day);
            assertTrue(card.getBalance().compareTo(card.getCreditLimit()) < 0, day);
        }
    }

    @Test
    @DisplayName("the car loan is partly paid off")
    void carLoanPartlyPaid(){
        for (String day : TODAYS) {
            Debt car = planFor(day).debts().stream().filter(d -> d.getKind() == Debt.Kind.loan).findFirst().orElseThrow();
            assertTrue(car.getBalance().signum() > 0, day);
            assertTrue(car.getBalance().compareTo(car.getOriginalBalance()) < 0, day);
        }
    }

    @Test
    @DisplayName("this month always has transactions and a budget, even on the 1st")
    void thisMonthIsNeverEmpty(){
        for (String day : TODAYS) {
            LocalDate thisMonth = LocalDate.parse(day).withDayOfMonth(1);
            DemoData.Plan plan = planFor(day);
            assertTrue(plan.transactions().stream().anyMatch(t -> !t.getTxnDate().isBefore(thisMonth)), day);
            assertEquals(DemoData.BUDGETS.size(), plan.budgets().stream().filter(b -> b.getMonth().equals(thisMonth)).count(), day);
        }
    }

    @Test
    @DisplayName("the most recent friday is a payday")
    void paidOnTheLatestFriday(){
        DemoData.Plan plan = planFor("2026-11-11");
        assertTrue(plan.transactions().stream()
            .anyMatch(t -> "income".equals(t.getType()) && t.getTxnDate().equals(LocalDate.parse("2026-11-06"))));
    }
}
