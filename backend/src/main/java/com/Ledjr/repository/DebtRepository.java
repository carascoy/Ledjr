package com.Ledjr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Debt;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    /** careful: across ALL users */
    List<Debt> findByCreditorName(String creditorName);

    List<Debt> findByUserId(Long userId);

    /** avalanche order (highest APR first) */
    List<Debt> findByUserIdOrderByAprDesc(Long userId);

    /** snowball order (smallest balance first) */
    List<Debt> findByUserIdOrderByBalanceAsc(Long userId);

    /** their own custom "attack" order */
    List<Debt> findByUserIdOrderByPriorityRank(Long userId);

}
