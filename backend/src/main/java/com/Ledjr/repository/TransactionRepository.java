package com.Ledjr.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Transaction;

/**
 * JpaRepository<Transaction, Long> gives these for free, no code needed:
 * save(transaction) — insert or update
 * findById(id)       — get one row by its id
 * findAll()           — get every row
 * deleteById(id)      — delete one row
 * count()             — how many rows exist
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Every transaction a specific user has logged. */
    List<Transaction> findByUserId(Long userId);

    /** All transactions filed under a specific category. */
    List<Transaction> findByCategoryId(Long categoryId);

    /** All transactions that are payments toward a specific debt. */
    List<Transaction> findByDebtId(Long debtId);

    /** One user's transactions within a date range — e.g. one calendar month, for the dashboard totals. */
    List<Transaction> findByUserIdAndTxn_dateBetween(Long userId, LocalDate start, LocalDate end);

    /** One user's transactions that haven't been assigned a category yet. */
    List<Transaction> findByUserIdAndCategoryIsNull(Long userId);

}
