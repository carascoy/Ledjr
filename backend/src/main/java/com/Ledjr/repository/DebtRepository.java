package com.Ledjr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Debt;

/**
 * JpaRepository<Debt, Long> gives these for free, no code needed:
 * save(debt)      — insert or update
 * findById(id)    — get one row by its id
 * findAll()       — get every row
 * deleteById(id)  — delete one row
 * count()         — how many rows exist
 */
public interface DebtRepository extends JpaRepository<Debt, Long> {

    /** All debts owed to a specific creditor, across every user. */
    List<Debt> findByCreditor_name(String creditor_name);

    /** Every debt a specific user owns. */
    List<Debt> findByUserId(Long userId);

    /** One user's debts, highest interest rate first — the avalanche payoff order. */
    List<Debt> findByUserIdOrderByAprDesc(Long userId);

    /** One user's debts, smallest balance first — the snowball payoff order. */
    List<Debt> findByUserIdOrderByBalanceAsc(Long userId);

    /** One user's debts in their own custom "attack" order. */
    List<Debt> findByUserIdOrderByPriorityRank(Long userId);

}
