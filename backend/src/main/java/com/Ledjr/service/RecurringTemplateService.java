package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.repository.RecurringTemplateRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RecurringTemplateService {

    private final RecurringTemplateRepository recurringTemplateRepository;
    private static final Logger log = LoggerFactory.getLogger(RecurringTemplateService.class);

    public RecurringTemplateService(RecurringTemplateRepository recurringTemplateRepository){
        this.recurringTemplateRepository = recurringTemplateRepository;
    }

    public RecurringTemplate newRecurringTemplate(AppUser user, Category category, String name, BigDecimal amount, RecurringTemplate.Frequency frequency, LocalDate nextDueDate){
        RecurringTemplate template = new RecurringTemplate();
        template.setUser(user);
        template.setCategory(category);
        template.setName(name);
        template.setAmount(amount);
        template.setFrequency(frequency);
        template.setNextDueDate(nextDueDate);
        recurringTemplateRepository.save(template);
        log.info("User id# {} created a new recurring template '{}' ({} {})", user.getId(), name, amount, frequency);
        return template;
    }

    public List<RecurringTemplate> findByUserId(Long userId){
        return recurringTemplateRepository.findByUserId(userId);
    }

    public List<RecurringTemplate> findByCategoryId(Long categoryId){
        return recurringTemplateRepository.findByCategoryId(categoryId);
    }

    public List<RecurringTemplate> findByName(String name){
        return recurringTemplateRepository.findByName(name);
    }

    public List<RecurringTemplate> findByFrequency(RecurringTemplate.Frequency frequency){
        return recurringTemplateRepository.findByFrequency(frequency);
    }

    public List<RecurringTemplate> findByNextDueDate(LocalDate date){
        return recurringTemplateRepository.findByNextDueDate(date);
    }

    /** Feeds the calendar view - everything a user has due in a date range. */
    public List<RecurringTemplate> findByUserIdDueBetween(Long userId, LocalDate start, LocalDate end){
        return recurringTemplateRepository.findByUserIdAndNextDueDateBetween(userId, start, end);
    }

}
