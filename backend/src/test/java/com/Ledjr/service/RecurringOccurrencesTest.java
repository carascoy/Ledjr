package com.Ledjr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.RecurringTemplate.Frequency;

/** when bills + paychecks land. every date gets counted from the start date so nothing drifts */
class RecurringOccurrencesTest {

    private static RecurringTemplate every(Frequency frequency, String start){
        RecurringTemplate t = new RecurringTemplate();
        t.setFrequency(frequency);
        t.setStartDate(LocalDate.parse(start));
        t.setAmount(new BigDecimal("10"));
        return t;
    }

    private static List<String> between(RecurringTemplate t, String from, String to){
        return RecurringTemplateService.occurrences(t, LocalDate.parse(from), LocalDate.parse(to))
            .stream().map(LocalDate::toString).toList();
    }

    @Test
    @DisplayName("a bill on the 31st lands on the last day of short months, then goes back to the 31st")
    void monthlyOnThe31st(){
        RecurringTemplate rent = every(Frequency.monthly, "2026-01-31");
        assertEquals(List.of("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30", "2026-05-31"),
            between(rent, "2026-01-01", "2026-05-31"));
    }

    @Test
    @DisplayName("leap years get Feb 29")
    void monthlyInALeapYear(){
        assertEquals(List.of("2028-02-29", "2028-03-31"), between(every(Frequency.monthly, "2028-01-31"), "2028-02-01", "2028-03-31"));
    }

    @Test
    @DisplayName("every 2 weeks can hit 3 times in one month")
    void biweeklyThreeTimes(){
        assertEquals(List.of("2026-10-02", "2026-10-16", "2026-10-30"),
            between(every(Frequency.biweekly, "2026-10-02"), "2026-10-01", "2026-10-31"));
    }

    @Test
    @DisplayName("every 2 weeks stays on schedule years after it started")
    void biweeklyYearsLater(){
        RecurringTemplate pay = every(Frequency.biweekly, "2020-01-03");
        LocalDate start = LocalDate.parse("2020-01-03");
        // work out the answer the slow way: every day in the month, keep the ones 14k days after the start
        List<String> expected = new ArrayList<>();
        for (LocalDate d = LocalDate.parse("2026-10-01"); !d.isAfter(LocalDate.parse("2026-10-31")); d = d.plusDays(1)) {
            if (ChronoUnit.DAYS.between(start, d) % 14 == 0) expected.add(d.toString());
        }
        assertEquals(expected, between(pay, "2026-10-01", "2026-10-31"));
        assertTrue(expected.size() >= 2);
    }

    @Test
    @DisplayName("weekly")
    void weekly(){
        assertEquals(List.of("2026-10-01", "2026-10-08", "2026-10-15", "2026-10-22", "2026-10-29"),
            between(every(Frequency.weekly, "2026-10-01"), "2026-10-01", "2026-10-31"));
    }

    @Test
    @DisplayName("yearly on Feb 29 is Feb 28 in normal years and back to the 29th in leap years")
    void yearlyOnLeapDay(){
        assertEquals(List.of("2028-02-29", "2029-02-28", "2030-02-28", "2031-02-28", "2032-02-29"),
            between(every(Frequency.annually, "2028-02-29"), "2028-01-01", "2032-12-31"));
    }

    @Test
    @DisplayName("nothing before the start date")
    void nothingBeforeStart(){
        assertEquals(List.of(), between(every(Frequency.monthly, "2026-12-01"), "2026-10-01", "2026-11-30"));
    }
}
