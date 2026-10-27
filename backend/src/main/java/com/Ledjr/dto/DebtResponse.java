package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtResponse(
    Long id,
    String kind,                 // "loan" or "card"
    String creditorName,
    BigDecimal balance,
    BigDecimal originalBalance,  // only really matters for loans
    BigDecimal creditLimit,      // cards only, null on loans
    BigDecimal apr,
    BigDecimal minPayment,
    Integer dueDate,
    LocalDate payoffTarget       // null = no plan
) {}
