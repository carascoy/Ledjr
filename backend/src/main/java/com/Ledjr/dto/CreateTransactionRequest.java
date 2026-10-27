package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
    Long userId,          // temporary until sessions (Phase 5)
    String type,          // "income" or "expense"
    BigDecimal amount,
    LocalDate txnDate,    // optional: defaults to today
    String description,
    Long categoryId       // optional: leave out for an uncategorized transaction
) {}
