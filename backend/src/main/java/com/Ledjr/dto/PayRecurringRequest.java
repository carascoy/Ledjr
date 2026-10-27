package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** POST /recurring/{id}/pay - "mark paid" on the calendar */
public record PayRecurringRequest(
    LocalDate dueDate,      // which due date this pays, e.g. the Nov 1 rent
    BigDecimal amount       // optional, defaults to the usual amount (for bills that vary)
) {}
