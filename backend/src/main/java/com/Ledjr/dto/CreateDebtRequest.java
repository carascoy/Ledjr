package com.Ledjr.dto;

import java.math.BigDecimal;

public record CreateDebtRequest(
    Long userId,                 // temporary until sessions (Phase 5)
    String kind,                 // "loan" or "card" (defaults to "loan")
    String creditorName,         // "Discover card", "Honda Civic loan"
    BigDecimal balance,          // what's owed right now
    BigDecimal originalBalance,  // loans, optional: what it started at (defaults to balance)
    BigDecimal creditLimit,      // cards, required: the card's limit
    BigDecimal apr,              // optional: annual rate in percent, e.g. 22.99
    BigDecimal minPayment,       // optional
    Integer dueDate              // optional: day of the month, 1-31
) {}
