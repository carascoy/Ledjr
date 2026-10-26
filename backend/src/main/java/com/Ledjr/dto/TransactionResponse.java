package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    String type,
    BigDecimal amount,
    LocalDate txnDate,
    String description
) {}