package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringResponse(
    Long id,
    String type,
    String name,
    BigDecimal amount,
    String frequency,
    LocalDate startDate,
    LocalDate nextDue,          // the next due date from today on
    Long categoryId,            // null when uncategorized
    String categoryName,
    Integer categoryColorSlot
) {}
