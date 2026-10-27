package com.Ledjr.dto;

import java.math.BigDecimal;

public record SetBudgetRequest(
    BigDecimal amount   // null or 0 = remove this category's budget for the month
) {}
