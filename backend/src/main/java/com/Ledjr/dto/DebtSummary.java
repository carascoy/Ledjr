package com.Ledjr.dto;

import java.math.BigDecimal;

/**
 * Everything the dashboard's Debts pill needs, in one response.
 * Loans and cards are measured differently, so each gets its own numbers.
 */
public record DebtSummary(
    BigDecimal totalOwed,        // every balance, loans + cards
    int debtCount,

    int loanCount,
    BigDecimal loanOriginal,     // what the loans started at
    BigDecimal loanRemaining,    // what's left on them
    int loanPercentPaid,         // 0-100

    int cardCount,
    BigDecimal cardBalance,      // what's on the cards
    BigDecimal cardLimit,        // their combined limits
    int cardPercentUsed          // utilization; can go over 100 if a card is over its limit
) {}
