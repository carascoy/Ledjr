package com.Ledjr.dto;

import java.math.BigDecimal;

/** one expense category's row on the budget page */
public record BudgetLine(
    Long categoryId,
    String name,
    int colorSlot,
    BigDecimal budgeted,          // null = no budget set for this month
    BigDecimal spent,
    BigDecimal suggested,         // null = nothing to go off of yet
    String suggestionReason       // "bill" (a recurring bill in this category) or "average" (last 3 months)
) {}
