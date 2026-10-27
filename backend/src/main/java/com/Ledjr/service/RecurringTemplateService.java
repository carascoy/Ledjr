package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.RecurringTemplate.Frequency;
import com.Ledjr.repository.RecurringTemplateRepository;

@Service
public class RecurringTemplateService {

    private final RecurringTemplateRepository recurringTemplateRepository;
    private static final Logger log = LoggerFactory.getLogger(RecurringTemplateService.class);

    public RecurringTemplateService(RecurringTemplateRepository recurringTemplateRepository){
        this.recurringTemplateRepository = recurringTemplateRepository;
    }

    public RecurringTemplate newRecurringTemplate(AppUser user, Category category, RecurringTemplate.Type type, String name,
                                                  BigDecimal amount, Frequency frequency, LocalDate startDate){
        RecurringTemplate template = new RecurringTemplate();
        template.setUser(user);
        apply(template, category, type, name, amount, frequency, startDate);
        recurringTemplateRepository.save(template);
        log.info("User id# {} created a new recurring template '{}' ({} {})", user.getId(), name, amount, frequency);
        return template;
    }

    /** edits change every due date, past unpaid ones too. paid ones keep their transactions */
    public RecurringTemplate update(RecurringTemplate template, Category category, RecurringTemplate.Type type, String name,
                                    BigDecimal amount, Frequency frequency, LocalDate startDate){
        apply(template, category, type, name, amount, frequency, startDate);
        recurringTemplateRepository.save(template);
        log.info("Recurring template id# {} edited", template.getId());
        return template;
    }

    /** the transactions it paid stay, they just stop pointing at it (ON DELETE SET NULL in V16) */
    public void delete(RecurringTemplate template){
        recurringTemplateRepository.delete(template);
        log.info("Recurring template id# {} deleted", template.getId());
    }

    private static void apply(RecurringTemplate t, Category category, RecurringTemplate.Type type, String name,
                              BigDecimal amount, Frequency frequency, LocalDate startDate){
        t.setCategory(category);
        t.setType(type);
        t.setName(name);
        t.setAmount(amount);
        t.setFrequency(frequency);
        t.setStartDate(startDate);
    }

    public Optional<RecurringTemplate> findById(Long id){
        if (id == null) {
            return Optional.empty();
        }
        return recurringTemplateRepository.findById(id);
    }

    public List<RecurringTemplate> findByUserId(Long userId){
        return recurringTemplateRepository.findByUserId(userId);
    }

    public List<RecurringTemplate> findByUserIdSorted(Long userId){
        return recurringTemplateRepository.findByUserIdOrderByNameAsc(userId);
    }

    public List<RecurringTemplate> findByCategoryId(Long categoryId){
        return recurringTemplateRepository.findByCategoryId(categoryId);
    }

    public List<RecurringTemplate> findByName(String name){
        return recurringTemplateRepository.findByName(name);
    }

    public List<RecurringTemplate> findByFrequency(Frequency frequency){
        return recurringTemplateRepository.findByFrequency(frequency);
    }

    /**
     * every due date of `t` between from and to (both included).
     * the nth one is always start + n steps counted from the START, never from the last one.
     * that way a monthly bill on the 31st lands on feb 28 but goes back to mar 31,
     * instead of drifting to the 28th forever
     */
    public static List<LocalDate> occurrences(RecurringTemplate t, LocalDate from, LocalDate to){
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = t.getStartDate();
        if (to.isBefore(start)) {
            return dates;
        }
        long n = stepsBefore(t.getFrequency(), start, from);
        while (true) {
            LocalDate date = nth(t.getFrequency(), start, n);
            if (date.isAfter(to)) {
                break;
            }
            if (!date.isBefore(from)) {
                dates.add(date);
            }
            n++;
        }
        return dates;
    }

    /** the next due date from `day` on (null if somehow none in the next 2 years) */
    public static LocalDate nextDue(RecurringTemplate t, LocalDate day){
        List<LocalDate> upcoming = occurrences(t, day, day.plusYears(2));
        return upcoming.isEmpty() ? null : upcoming.get(0);
    }

    private static LocalDate nth(Frequency frequency, LocalDate start, long n){
        return switch (frequency) {
            case daily -> start.plusDays(n);
            case weekly -> start.plusWeeks(n);
            case biweekly -> start.plusWeeks(2 * n);
            case monthly -> start.plusMonths(n);
            case annually -> start.plusYears(n);
        };
    }

    /** jump close to `from` instead of looping up from the start every time (one step early to be safe) */
    private static long stepsBefore(Frequency frequency, LocalDate start, LocalDate from){
        if (!from.isAfter(start)) {
            return 0;
        }
        long steps = switch (frequency) {
            case daily -> ChronoUnit.DAYS.between(start, from);
            case weekly -> ChronoUnit.WEEKS.between(start, from);
            case biweekly -> ChronoUnit.WEEKS.between(start, from) / 2;
            case monthly -> ChronoUnit.MONTHS.between(start, from);
            case annually -> ChronoUnit.YEARS.between(start, from);
        };
        return Math.max(0, steps - 1);
    }
}
