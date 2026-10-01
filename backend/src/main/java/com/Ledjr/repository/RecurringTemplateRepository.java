package com.Ledjr.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.RecurringTemplate;

/**
 * JpaRepository<RecurringTemplate, Long> gives these for free, no code needed:
 * save(recurringTemplate) — insert or update
 * findById(id)            — get one row by its id
 * findAll()                — get every row
 * deleteById(id)           — delete one row
 * count()                  — how many rows exist
 */
public interface RecurringTemplateRepository extends JpaRepository<RecurringTemplate, Long> {

    /** All recurring templates filed under a specific category. */
    List<RecurringTemplate> findByCategoryId(Long categoryId);

    /** All recurring templates with a given name, across every user. */
    List<RecurringTemplate> findByName(String name);

    /** All recurring templates with a given frequency (daily/weekly/etc.), across every user. */
    List<RecurringTemplate> findByFrequency(RecurringTemplate.Frequency frequency);

    /** All recurring templates due on an exact date, across every user. */
    List<RecurringTemplate> findByNextDueDate(LocalDate date);

    /** Every recurring template a specific user owns. */
    List<RecurringTemplate> findByUserId(Long userId);

    /** One user's recurring templates due within a date range — feeds the calendar view. */
    List<RecurringTemplate> findByUserIdAndNextDueDateBetween(Long userId, LocalDate start, LocalDate end);

}
