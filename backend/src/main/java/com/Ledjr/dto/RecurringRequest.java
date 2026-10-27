package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** POST /recurring and PUT /recurring/{id} (edit resends the whole thing) */
public record RecurringRequest(
    String type,            // "income" or "expense"
    String name,            // "Rent", "Spotify", "Paycheck"
    BigDecimal amount,
    String frequency,       // "weekly", "biweekly", "monthly" or "annually"
    LocalDate startDate,    // the first due date, every other one is counted from it
    Long categoryId         // optional
) {}
