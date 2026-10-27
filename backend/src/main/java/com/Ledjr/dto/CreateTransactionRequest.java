package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
    Long userId,          // temporary until sessions (Phase 5)
    String type,          // "income", "expense" or "payment" (money sent to a debt)
    BigDecimal amount,
    LocalDate txnDate,    // optional: defaults to today
    String description,
    Long categoryId,      // optional: leave out for an uncategorized transaction
    Long debtId           // payment: the debt being paid (required); expense: the card it was put on (optional)
) {}
