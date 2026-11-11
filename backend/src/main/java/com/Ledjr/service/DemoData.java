package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Budget;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.RecurringTemplate.Frequency;
import com.Ledjr.entity.Transaction;

/** 3 months of fake history ending today, so a demo always looks current. DemoService saves it */
public final class DemoData {

    public record Plan(List<RecurringTemplate> recurring, List<Debt> debts, List<Transaction> transactions, List<Budget> budgets) {}

    static final Map<String, String> BUDGETS = new LinkedHashMap<>();
    static {
        BUDGETS.put("Rent", "1200");
        BUDGETS.put("Groceries", "450");
        BUDGETS.put("Dining Out", "160");
        BUDGETS.put("Transportation", "160");
        BUDGETS.put("Utilities", "140");
        BUDGETS.put("Shopping", "150");
        BUDGETS.put("Entertainment", "70");
    }

    static final BigDecimal CAR_LOAN_ORIGINAL = new BigDecimal("15000.00");
    static final BigDecimal CAR_LOAN_AT_START = new BigDecimal("11800.00");
    static final BigDecimal CARD_AT_START = new BigDecimal("1500.00");

    private DemoData(){}

    static LocalDate historyStart(LocalDate today){
        return today.withDayOfMonth(1).minusMonths(2);
    }

    public static Plan build(AppUser user, Map<String, Category> categories, LocalDate today, long seed){
        LocalDate start = historyStart(today);
        // backdated so the calendar shows past due dates as paid instead of hiding them
        LocalDateTime addedAt = start.atTime(9, 0);
        Random random = new Random(seed);

        List<RecurringTemplate> recurring = new ArrayList<>();
        List<Debt> debts = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        List<Budget> budgets = new ArrayList<>();

        LocalDate payday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        while (!payday.minusWeeks(2).isBefore(start)) {
            payday = payday.minusWeeks(2);
        }
        recurring.add(recurring(user, categories.get("Paycheck"), "Paycheck", "1650.00",
            RecurringTemplate.Type.income, Frequency.biweekly, payday, addedAt));

        recurring.add(monthly(user, categories.get("Rent"), "Rent", "1200.00", start, 1, addedAt));
        recurring.add(monthly(user, categories.get("Utilities"), "Phone", "45.00", start, 12, addedAt));
        recurring.add(monthly(user, categories.get("Utilities"), "Electric", "84.50", start, 18, addedAt));
        recurring.add(monthly(user, categories.get("Entertainment"), "Spotify", "11.99", start, 22, addedAt));

        for (RecurringTemplate r : recurring) {
            String type = r.getType() == RecurringTemplate.Type.income ? "income" : "expense";
            for (LocalDate due : RecurringTemplateService.occurrences(r, start, today)) {
                Transaction t = transaction(user, r.getCategory(), null, type, r.getAmount(), due, r.getName());
                t.setRecurringTemplate(r);
                t.setDueDate(due);
                transactions.add(t);
            }
        }

        Debt car = new Debt();
        car.setUser(user);
        car.setKind(Debt.Kind.loan);
        car.setCreditorName("Car Loan");
        car.setOriginalBalance(CAR_LOAN_ORIGINAL);
        car.setApr(new BigDecimal("6.90"));
        car.setMinPayment(new BigDecimal("313.00"));
        car.setDueDate(5);
        car.setCreatedAt(addedAt);
        debts.add(car);

        Debt card = new Debt();
        card.setUser(user);
        card.setKind(Debt.Kind.card);
        card.setCreditorName("Visa Card");
        card.setOriginalBalance(CARD_AT_START);
        card.setCreditLimit(new BigDecimal("3000.00"));
        card.setApr(new BigDecimal("24.99"));
        card.setMinPayment(new BigDecimal("40.00"));
        card.setDueDate(15);
        card.setCreatedAt(addedAt);
        debts.add(card);

        BigDecimal carBalance = CAR_LOAN_AT_START;
        BigDecimal cardBalance = CARD_AT_START;

        for (LocalDate month = start; !month.isAfter(today); month = month.plusMonths(1)) {
            LocalDate carDue = month.withDayOfMonth(5);
            if (!carDue.isAfter(today)) {
                transactions.add(transaction(user, null, car, "payment", car.getMinPayment(), carDue, "Car loan payment"));
                carBalance = carBalance.subtract(car.getMinPayment());
            }
            LocalDate cardDue = month.withDayOfMonth(15);
            if (!cardDue.isAfter(today)) {
                BigDecimal paid = new BigDecimal("150.00");
                transactions.add(transaction(user, null, card, "payment", paid, cardDue, "Visa payment"));
                cardBalance = cardBalance.subtract(paid);
            }
        }

        String[] groceryStores = {"Market Basket", "Trader Joe's", "Stop & Shop"};
        String[] restaurants = {"Chipotle", "Panera", "Sweetgreen", "Pho place", "Pizza night", "Dunkin'"};
        int grocery = 0;
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SUNDAY) {
                transactions.add(transaction(user, categories.get("Groceries"), null, "expense",
                    money(random, 65, 135), day, groceryStores[grocery++ % groceryStores.length]));
            }
            if ((dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.SATURDAY) && random.nextInt(10) < 8) {
                transactions.add(transaction(user, categories.get("Dining Out"), null, "expense",
                    money(random, 12, 48), day, restaurants[random.nextInt(restaurants.length)]));
            }
            if (day.toEpochDay() % 9 == 0) {
                transactions.add(transaction(user, categories.get("Transportation"), null, "expense",
                    money(random, 38, 54), day, "Gas"));
            }
            if (day.getDayOfMonth() == 8) {
                transactions.add(transaction(user, categories.get("Shopping"), null, "expense",
                    money(random, 25, 70), day, "Target"));
            }
            if (day.getDayOfMonth() == 24) {
                BigDecimal amount = money(random, 30, 80);
                transactions.add(transaction(user, categories.get("Shopping"), card, "expense", amount, day, "Amazon"));
                cardBalance = cardBalance.add(amount);
            }
            if (day.getDayOfMonth() == 20) {
                transactions.add(transaction(user, categories.get("Entertainment"), null, "expense",
                    new BigDecimal("28.00"), day, "Movie tickets"));
            }
        }

        car.setBalance(carBalance);
        card.setBalance(cardBalance);

        for (LocalDate month = start; !month.isAfter(today); month = month.plusMonths(1)) {
            for (Map.Entry<String, String> entry : BUDGETS.entrySet()) {
                Category category = categories.get(entry.getKey());
                if (category == null) continue;
                Budget budget = new Budget();
                budget.setUser(user);
                budget.setCategory(category);
                budget.setMonth(month);
                budget.setAmount(new BigDecimal(entry.getValue()));
                budgets.add(budget);
            }
        }

        return new Plan(recurring, debts, transactions, budgets);
    }

    private static RecurringTemplate monthly(AppUser user, Category category, String name, String amount,
                                             LocalDate start, int day, LocalDateTime addedAt){
        return recurring(user, category, name, amount, RecurringTemplate.Type.expense, Frequency.monthly,
            start.withDayOfMonth(day), addedAt);
    }

    private static RecurringTemplate recurring(AppUser user, Category category, String name, String amount,
                                               RecurringTemplate.Type type, Frequency frequency, LocalDate firstDue,
                                               LocalDateTime addedAt){
        RecurringTemplate r = new RecurringTemplate();
        r.setUser(user);
        r.setCategory(category);
        r.setName(name);
        r.setAmount(new BigDecimal(amount));
        r.setType(type);
        r.setFrequency(frequency);
        r.setStartDate(firstDue);
        r.setCreatedAt(addedAt);
        return r;
    }

    private static Transaction transaction(AppUser user, Category category, Debt debt, String type,
                                           BigDecimal amount, LocalDate date, String description){
        Transaction t = new Transaction();
        t.setUser(user);
        t.setCategory(category);
        t.setDebt(debt);
        t.setType(type);
        t.setAmount(amount);
        t.setTxnDate(date);
        t.setDescription(description);
        return t;
    }

    private static BigDecimal money(Random random, int min, int max){
        long cents = min * 100L + random.nextInt((max - min) * 100 + 1);
        return BigDecimal.valueOf(cents, 2);
    }
}
