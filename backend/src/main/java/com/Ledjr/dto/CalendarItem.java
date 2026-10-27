package com.Ledjr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** one thing due on one day of the calendar: a recurring bill/paycheck or a debt's payment */
public record CalendarItem(
    String source,              // "recurring" or "debt"
    Long sourceId,              // the recurring template's id or the debt's id
    LocalDate date,             // the due date
    String type,                // "income", "expense" or "debt"
    String name,
    BigDecimal amount,          // expected amount (a debt's minimum payment, can be null)
    boolean paid,
    Long transactionId,         // the transaction that paid it, null if not paid
    BigDecimal paidAmount,      // what was actually paid (bills that vary)
    String categoryName,        // null when uncategorized or a debt
    Integer categoryColorSlot
) {}
