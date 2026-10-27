package com.Ledjr.dto;

import java.math.BigDecimal;

/**
 * everything the Debts pill needs in one call.
 * loans and cards get measured differently so they each get their own numbers
 */
public record DebtSummary(
    BigDecimal totalOwed,        // loans + cards
    int debtCount,

    int loanCount,
    BigDecimal loanOriginal,     // what the loans started at
    BigDecimal loanRemaining,
    int loanPercentPaid,         // 0-100

    int cardCount,
    BigDecimal cardBalance,
    BigDecimal cardLimit,        // all the limits added up
    int cardPercentUsed          // utilization, can go past 100 if a card's over its limit
) {}
