package com.Ledjr.service;

import static com.Ledjr.Fakes.assertMoney;
import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.Ledjr.dto.CalendarItem;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;

/**
 * a card's minimum disappears from the calendar once that month's payments cover it.
 * card due on the 4th, minimum $60, tracked since Oct 1, calendar shows Oct + Nov
 */
class CardMinimumTest {

    private long nextId = 1;

    /** the card's due dates on the calendar, given its minimum, when it was added and its payments (date, amount, date, amount...) */
    private List<CalendarItem> dueDates(String minimum, String addedOn, String... payments){
        Debt card = new Debt();
        card.setId(2L);
        card.setCreditorName("Capital One");
        card.setDueDate(4);
        card.setBalance(new BigDecimal("3579"));
        card.setMinPayment(minimum == null ? null : new BigDecimal(minimum));
        setCreatedAt(card, LocalDate.parse(addedOn));

        List<Transaction> txns = new ArrayList<>();
        for (int i = 0; i < payments.length; i += 2) {
            Transaction t = new Transaction();
            t.setId(nextId++);
            t.setType("payment");
            t.setDebt(card);
            t.setTxnDate(LocalDate.parse(payments[i]));
            t.setAmount(new BigDecimal(payments[i + 1]));
            txns.add(t);
        }

        DebtRepository debts = fake(DebtRepository.class, Map.of("findByUserId", args -> List.of(card)));
        TransactionRepository transactions = fake(TransactionRepository.class, Map.of("findByUserIdAndTxnDateBetween", args -> txns));
        RecurringTemplateRepository recurring = fake(RecurringTemplateRepository.class, Map.of());
        CalendarService calendar = new CalendarService(new RecurringTemplateService(recurring),
            new DebtService(debts, transactions), new TransactionService(transactions, debts));
        return calendar.itemsBetween(7L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-30"));
    }

    // createdAt has no setter (the database fills it in), so tests set it directly
    private static void setCreatedAt(Debt debt, LocalDate day){
        try {
            var field = Debt.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(debt, day.atStartOfDay());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CalendarItem on(List<CalendarItem> items, String date){
        return items.stream().filter(i -> i.date().toString().equals(date)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("two small payments that add up to the minimum count")
    void paymentsAddUp(){
        List<CalendarItem> items = dueDates("60", "2026-10-01", "2026-10-03", "20", "2026-10-10", "40");
        assertTrue(on(items, "2026-10-04").paid());
        assertMoney("60", on(items, "2026-10-04").paidAmount());
        assertFalse(on(items, "2026-11-04").paid());
    }

    @Test
    @DisplayName("not enough yet: still due, and shows what's been paid so far")
    void partlyPaid(){
        CalendarItem oct = on(dueDates("60", "2026-10-01", "2026-10-03", "20"), "2026-10-04");
        assertFalse(oct.paid());
        assertMoney("20", oct.paidAmount());
    }

    @Test
    @DisplayName("a late payment still covers that month")
    void latePayment(){
        assertTrue(on(dueDates("60", "2026-10-01", "2026-10-20", "60"), "2026-10-04").paid());
    }

    @Test
    @DisplayName("paying next month early counts for next month, and one payment never counts twice")
    void earlyPaymentGoesToNextMonth(){
        List<CalendarItem> items = dueDates("60", "2026-10-01", "2026-10-03", "60", "2026-10-30", "60");
        assertTrue(on(items, "2026-10-04").paid());
        assertTrue(on(items, "2026-11-04").paid());
    }

    @Test
    @DisplayName("one big payment covers its month but doesn't spill into the next")
    void bigPaymentDoesNotSplit(){
        List<CalendarItem> items = dueDates("60", "2026-10-01", "2026-10-03", "200");
        assertTrue(on(items, "2026-10-04").paid());
        assertFalse(on(items, "2026-11-04").paid());
    }

    @Test
    @DisplayName("no minimum set: any payment counts")
    void noMinimum(){
        assertTrue(on(dueDates(null, "2026-10-01", "2026-10-15", "5"), "2026-10-04").paid());
    }

    @Test
    @DisplayName("due dates from before the card was added don't show up as unpaid")
    void skipsDatesBeforeItWasAdded(){
        List<CalendarItem> items = dueDates("60", "2026-10-10", "2026-10-20", "60");
        assertEquals(1, items.size());
        assertEquals("2026-11-04", items.get(0).date().toString());
        assertTrue(items.get(0).paid());
    }

    @Test
    @DisplayName("nothing paid: due, and nothing paid so far")
    void nothingPaid(){
        CalendarItem oct = on(dueDates("60", "2026-10-01"), "2026-10-04");
        assertFalse(oct.paid());
        assertNull(oct.paidAmount());
    }
}
