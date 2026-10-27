package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
    Long userId,          // temp until sessions (phase 5)
    String type,          // "income", "expense" or "payment" (paying off a debt)
    BigDecimal amount,
    LocalDate txnDate,    // optional, defaults to today
    String description,
    Long categoryId,      // optional, leave out for uncategorized
    Long debtId           // payment: which debt (required). expense: which card (optional)
) {}
