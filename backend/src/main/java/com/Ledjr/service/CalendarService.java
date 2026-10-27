package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Ledjr.dto.CalendarItem;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.Transaction;

/**
 * builds the calendar: every recurring bill/paycheck due date + every debt's due day in a range,
 * each marked paid or not. nothing is stored per day, it's all worked out from the recurring
 * items + the transactions that paid them
 */
@Service
public class CalendarService {

    private final RecurringTemplateService recurringTemplateService;
    private final DebtService debtService;
    private final TransactionService transactionService;

    public CalendarService(RecurringTemplateService recurringTemplateService, DebtService debtService, TransactionService transactionService){
        this.recurringTemplateService = recurringTemplateService;
        this.debtService = debtService;
        this.transactionService = transactionService;
    }

    public List<CalendarItem> itemsBetween(Long userId, LocalDate from, LocalDate to){
        List<CalendarItem> items = new ArrayList<>();
        items.addAll(recurringItems(userId, from, to));
        items.addAll(debtItems(userId, from, to));
        items.sort(Comparator.comparing(CalendarItem::date).thenComparing(CalendarItem::name));
        return items;
    }

    private List<CalendarItem> recurringItems(Long userId, LocalDate from, LocalDate to){
        // "templateId|date" -> the transaction that paid it
        Map<String, Transaction> paid = new HashMap<>();
        for (Transaction t : transactionService.findPaidRecurringBetween(userId, from, to)) {
            paid.put(key(t.getRecurringTemplate().getId(), t.getDueDate()), t);
        }

        List<CalendarItem> items = new ArrayList<>();
        for (RecurringTemplate r : recurringTemplateService.findByUserId(userId)) {
            Category c = r.getCategory();
            // due dates before the month you added it only show if you paid them
            LocalDate tracked = r.getCreatedAt().toLocalDate().withDayOfMonth(1);
            for (LocalDate date : RecurringTemplateService.occurrences(r, from, to)) {
                Transaction p = paid.get(key(r.getId(), date));
                if (p == null && date.isBefore(tracked)) {
                    continue;
                }
                items.add(new CalendarItem(
                    "recurring", r.getId(), date, r.getType().name(), r.getName(), r.getAmount(),
                    p != null, p == null ? null : p.getId(), p == null ? null : p.getAmount(),
                    c == null ? null : c.getName(), c == null ? null : c.getColorSlot()));
            }
        }
        return items;
    }

    /**
     * a debt shows on its due day every month, paid once that month's minimum is covered.
     *
     * payments add up toward the minimum ($20 of a $60 minimum = still $40 left), and each payment
     * goes to the OLDEST due date that still needs it, like a real card statement:
     *   - a late payment (oct 20 for the oct 4 due date) covers october first
     *   - an early one (oct 30 for nov 4) counts toward november
     * a due date can take payments from the day after the previous due date up to the day before
     * the next one, and one payment never counts twice.
     *
     * only due dates from when you added the debt count. earlier ones aren't shown and don't
     * soak up payments (otherwise "oldest first" would hand an oct 3 payment to a september
     * due date from before you used the app). paid-off debts don't show
     */
    private List<CalendarItem> debtItems(Long userId, LocalDate from, LocalDate to){
        // a month of padding on each side so the due dates at the edges see all their payments
        YearMonth first = YearMonth.from(from).minusMonths(1);
        YearMonth last = YearMonth.from(to).plusMonths(1);
        List<Transaction> payments = transactionService
            .findByUserIdBetweenDates(userId, first.minusMonths(1).atDay(1), last.atEndOfMonth())
            .stream()
            .filter(t -> "payment".equals(t.getType()) && t.getDebt() != null)
            .sorted(Comparator.comparing(Transaction::getTxnDate).thenComparing(Transaction::getId))
            .toList();

        List<CalendarItem> items = new ArrayList<>();
        for (Debt debt : debtService.findByUserId(userId)) {
            if (debt.getDueDate() == null || debt.getBalance().signum() <= 0) {
                continue;
            }
            BigDecimal minimum = debt.getMinPayment();
            LocalDate tracked = debt.getCreatedAt().toLocalDate();
            // this debt's payments that haven't been used for a due date yet
            List<Transaction> unused = new ArrayList<>(payments.stream()
                .filter(t -> t.getDebt().getId().equals(debt.getId()))
                .toList());

            for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
                LocalDate due = dueDateIn(debt, m);
                if (due.isBefore(tracked)) {
                    continue;
                }
                LocalDate windowStart = dueDateIn(debt, m.minusMonths(1)).plusDays(1);
                LocalDate windowEnd = dueDateIn(debt, m.plusMonths(1)).minusDays(1);

                BigDecimal covered = BigDecimal.ZERO;
                Transaction latest = null;
                Iterator<Transaction> it = unused.iterator();
                while (it.hasNext() && !isCovered(covered, minimum)) {
                    Transaction t = it.next();
                    if (t.getTxnDate().isBefore(windowStart) || t.getTxnDate().isAfter(windowEnd)) {
                        continue;
                    }
                    covered = covered.add(t.getAmount());
                    latest = t;
                    it.remove();
                }

                // only the months you asked for get sent back, the padding was just for counting
                if (due.isBefore(from) || due.isAfter(to)) {
                    continue;
                }
                items.add(new CalendarItem(
                    "debt", debt.getId(), due, "debt", debt.getCreditorName(), minimum,
                    isCovered(covered, minimum),
                    latest == null ? null : latest.getId(),
                    covered.signum() == 0 ? null : covered,
                    null, null));
            }
        }
        return items;
    }

    /** due on the 31st in a 30-day month = the 30th */
    private static LocalDate dueDateIn(Debt debt, YearMonth month){
        return month.atDay(Math.min(debt.getDueDate(), month.lengthOfMonth()));
    }

    /** no minimum set = any payment counts */
    private static boolean isCovered(BigDecimal covered, BigDecimal minimum){
        if (minimum == null || minimum.signum() <= 0) {
            return covered.signum() > 0;
        }
        return covered.compareTo(minimum) >= 0;
    }

    private static String key(Long templateId, LocalDate date){
        return templateId + "|" + date;
    }
}
