package com.Ledjr.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Ledjr.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByCategoryId(Long categoryId);

    /** everything tied to a debt: payments + purchases on it if it's a card */
    List<Transaction> findByDebtId(Long debtId);

    /** e.g. one month for the dashboard totals */
    List<Transaction> findByUserIdAndTxnDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndCategoryIsNull(Long userId);

    /**
     * newest first, same-day ties go to whatever was added last (higher id).
     * JPQL uses the java class/field names (Transaction, txnDate), NOT the table/column names
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND t.txnDate BETWEEN :start AND :end
        ORDER BY t.txnDate DESC, t.id DESC
        """)
    List<Transaction> findForPeriodNewestFirst(
        @Param("userId") Long userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end);

    /** the paid recurring items whose due date falls in a range, for the calendar */
    List<Transaction> findByUserIdAndRecurringTemplateIsNotNullAndDueDateBetween(Long userId, LocalDate start, LocalDate end);

    /** has this due date of this recurring item already been paid? */
    boolean existsByRecurringTemplateIdAndDueDate(Long recurringTemplateId, LocalDate dueDate);

}
