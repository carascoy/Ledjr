package com.Ledjr.dto;

import java.math.BigDecimal;

public record CreateDebtRequest(
    String kind,                 // "loan" or "card", defaults to loan
    String creditorName,
    BigDecimal balance,
    BigDecimal originalBalance,  // loans only, optional: what it started at (defaults to balance)
    BigDecimal creditLimit,      // cards only, required
    BigDecimal apr,              // optional, yearly % like 22.99
    BigDecimal minPayment,       // optional
    Integer dueDate              // optional, day of the month 1-31
) {}
