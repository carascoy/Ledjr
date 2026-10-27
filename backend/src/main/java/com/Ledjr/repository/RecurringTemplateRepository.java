package com.Ledjr.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.RecurringTemplate;

public interface RecurringTemplateRepository extends JpaRepository<RecurringTemplate, Long> {

    List<RecurringTemplate> findByCategoryId(Long categoryId);

    /** careful: across ALL users */
    List<RecurringTemplate> findByName(String name);

    /** careful: across ALL users */
    List<RecurringTemplate> findByFrequency(RecurringTemplate.Frequency frequency);

    /** careful: across ALL users */
    List<RecurringTemplate> findByNextDueDate(LocalDate date);

    List<RecurringTemplate> findByUserId(Long userId);

    /** for the calendar view */
    List<RecurringTemplate> findByUserIdAndNextDueDateBetween(Long userId, LocalDate start, LocalDate end);

}
