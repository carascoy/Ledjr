package com.Ledjr.service;

import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.Ledjr.dto.CalendarItem;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.RecurringTemplate.Frequency;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;

/**
 * a bill or paycheck only shows on the calendar from the day you added it on.
 * phone bill due on the 5th, added Oct 27, calendar shows Oct + Nov
 */
class RecurringCalendarTest {

    /** the bill's dates on the calendar, given when it was added and which due dates got paid */
    private List<String> datesShown(Frequency frequency, String start, String addedOn, String... paidDueDates){
        RecurringTemplate bill = new RecurringTemplate();
        bill.setId(5L);
        bill.setName("Phone");
        bill.setType(RecurringTemplate.Type.expense);
        bill.setAmount(new BigDecimal("40"));
        bill.setFrequency(frequency);
        bill.setStartDate(LocalDate.parse(start));
        setCreatedAt(bill, LocalDate.parse(addedOn));

        List<Transaction> paid = new ArrayList<>();
        long id = 1;
        for (String due : paidDueDates) {
            Transaction t = new Transaction();
            t.setId(id++);
            t.setType("expense");
            t.setRecurringTemplate(bill);
            t.setDueDate(LocalDate.parse(due));
            t.setTxnDate(LocalDate.parse(due));
            t.setAmount(new BigDecimal("40"));
            paid.add(t);
        }

        RecurringTemplateRepository recurring = fake(RecurringTemplateRepository.class, Map.of("findByUserId", args -> List.of(bill)));
        TransactionRepository transactions = fake(TransactionRepository.class,
            Map.of("findByUserIdAndRecurringTemplateIsNotNullAndDueDateBetween", args -> paid));
        DebtRepository debts = fake(DebtRepository.class, Map.of());
        CalendarService calendar = new CalendarService(new RecurringTemplateService(recurring),
            new DebtService(debts, transactions), new TransactionService(transactions, debts));
        return calendar.itemsBetween(7L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-30"))
            .stream().map(CalendarItem::date).map(LocalDate::toString).toList();
    }

    // createdAt has no setter (the database fills it in), so tests set it directly
    private static void setCreatedAt(RecurringTemplate t, LocalDate day){
        try {
            var field = RecurringTemplate.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(t, day.atStartOfDay());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("a bill added on the 27th doesn't show as missed earlier that month")
    void nothingBeforeYouAddedIt(){
        assertEquals(List.of("2026-11-05"), datesShown(Frequency.monthly, "2026-01-05", "2026-10-27"));
    }

    @Test
    @DisplayName("a bill due the day you add it still shows")
    void dueTheSameDay(){
        assertEquals(List.of("2026-10-27", "2026-11-27"), datesShown(Frequency.monthly, "2026-01-27", "2026-10-27"));
    }

    @Test
    @DisplayName("paychecks every 2 weeks only show from sign-up on")
    void biweeklyPaychecks(){
        // setup walks the start back to before the 1st so the budget counts the whole month,
        // but the calendar only shows Oct 30 on
        assertEquals(List.of("2026-10-30", "2026-11-13", "2026-11-27"),
            datesShown(Frequency.biweekly, "2026-09-18", "2026-10-27"));
    }

    @Test
    @DisplayName("an earlier due date you actually paid still shows")
    void paidOnesStay(){
        List<String> dates = datesShown(Frequency.monthly, "2026-01-05", "2026-10-27", "2026-10-05");
        assertTrue(dates.contains("2026-10-05"));
        assertEquals(List.of("2026-10-05", "2026-11-05"), dates);
    }
}
