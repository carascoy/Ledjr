package com.Ledjr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.RecurringTemplate;

public interface RecurringTemplateRepository extends JpaRepository<RecurringTemplate, Long> {

    List<RecurringTemplate> findByCategoryId(Long categoryId);

    /** careful: across ALL users */
    List<RecurringTemplate> findByName(String name);

    /** careful: across ALL users */
    List<RecurringTemplate> findByFrequency(RecurringTemplate.Frequency frequency);

    List<RecurringTemplate> findByUserId(Long userId);

    /** what the Recurring page lists */
    List<RecurringTemplate> findByUserIdOrderByNameAsc(Long userId);

}
