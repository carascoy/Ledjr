package com.Ledjr.dto;

import java.math.BigDecimal;

public record DebtResponse(
    Long id,
    String kind,                 // "loan" or "card"
    String creditorName,
    BigDecimal balance,
    BigDecimal originalBalance,  // meaningful for loans
    BigDecimal creditLimit,      // cards only, null for loans
    BigDecimal apr,
    BigDecimal minPayment,
    Integer dueDate
) {}
