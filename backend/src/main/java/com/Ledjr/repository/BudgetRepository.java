package com.Ledjr.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /** one month's budget. month = the 1st */
    List<Budget> findByUserIdAndMonth(Long userId, LocalDate month);

    Optional<Budget> findByCategoryIdAndMonth(Long categoryId, LocalDate month);

    /** every finished month, for adding up savings */
    List<Budget> findByUserIdAndMonthBefore(Long userId, LocalDate month);

    boolean existsByUserIdAndMonth(Long userId, LocalDate month);

    /** every month's budget for one category, for when the category gets deleted */
    List<Budget> findByCategoryId(Long categoryId);

    /** everything, for deleting the account */
    List<Budget> findByUserId(Long userId);

}
