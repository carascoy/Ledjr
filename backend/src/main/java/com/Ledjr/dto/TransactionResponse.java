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
    String categoryName,      // null when uncategorized
    Integer categoryColorSlot, // null when uncategorized; 0-7 otherwise
    Long debtId,              // the debt paid (payment) or the card charged (expense); null otherwise
    String debtName
) {}