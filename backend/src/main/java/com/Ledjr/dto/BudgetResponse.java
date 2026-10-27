package com.Ledjr.dto;

import java.math.BigDecimal;
import java.util.List;

/** what GET /budget?month=2026-10 sends back */
public record BudgetResponse(
    String month,                   // "2026-10"
    List<BudgetLine> lines,         // every expense category, budgeted ones first
    BigDecimal totalBudgeted,
    BigDecimal totalSpent,          // only in categories that have a budget
    BigDecimal unbudgetedSpent,     // categories with no budget + uncategorized
    BigDecimal income,              // income actually logged this month
    BigDecimal savedTotal,          // unspent budget from every finished month
    boolean canCopyLastMonth,       // this month is empty and last month isn't

    // the "how much can I budget" estimate
    BigDecimal expectedIncome,      // what the estimate starts from
    String incomeBasis,             // "recurring", "logged", "last-month" or "none"
    int paycheckCount,              // recurring income hits this month, for "2 paychecks"
    BigDecimal debtMinimums,        // minimums on debts that still have a balance
    BigDecimal otherBills,          // recurring bills with no category (can't be suggested anywhere)
    BigDecimal canBudget            // expectedIncome - debtMinimums - otherBills
) {}
