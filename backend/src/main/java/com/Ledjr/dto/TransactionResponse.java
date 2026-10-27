package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    String type,
    BigDecimal amount,
    LocalDate txnDate,
    String description,
    Long categoryId,          // null when uncategorized
    String categoryName,
    Integer categoryColorSlot, // null if uncategorized, else 0-7
    Long debtId,              // debt paid (payment) or card charged (expense), null otherwise
    String debtName
) {}