package com.Ledjr.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.repository.AppUserRepository;
import com.Ledjr.repository.BudgetRepository;
import com.Ledjr.repository.CategoryRepository;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;

/** "Try the demo" accounts: made on click, deleted after a day */
@Service
public class DemoService {

    static final Duration LIFETIME = Duration.ofHours(24);
    private static final String ID_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    private final AppUserRepository users;
    private final CategoryService categoryService;
    private final CategoryRepository categories;
    private final RecurringTemplateRepository recurring;
    private final DebtRepository debts;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final SessionKiller sessionKiller;

    public DemoService(AppUserRepository users, CategoryService categoryService, CategoryRepository categories,
                       RecurringTemplateRepository recurring, DebtRepository debts, TransactionRepository transactions,
                       BudgetRepository budgets, PasswordEncoder passwordEncoder, AccountService accountService,
                       SessionKiller sessionKiller){
        this.users = users;
        this.categoryService = categoryService;
        this.categories = categories;
        this.recurring = recurring;
        this.debts = debts;
        this.transactions = transactions;
        this.budgets = budgets;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.sessionKiller = sessionKiller;
    }

    @Transactional
    public AppUser create(LocalDate today){
        AppUser user = new AppUser();
        user.setEmail("demo-" + randomId(12) + "@demo.ledjr.dev");
        // a password nobody ever sees, so the only way in is the session this click creates
        user.setPasswordHash(passwordEncoder.encode(randomId(32)));
        user.setFirstName("Alex");
        user.setLastName("Rivera");
        user.setSetupDone(true);
        user.setDemo(true);
        users.save(user);

        categoryService.seedDefaults(user);
        Map<String, Category> byName = new HashMap<>();
        for (Category c : categories.findByUserId(user.getId())) {
            byName.put(c.getName(), c);
        }

        DemoData.Plan plan = DemoData.build(user, byName, today, RANDOM.nextLong());
        recurring.saveAll(plan.recurring());
        debts.saveAll(plan.debts());
        transactions.saveAll(plan.transactions());
        budgets.saveAll(plan.budgets());

        log.info("Demo account id# {} created", user.getId());
        return user;
    }

    @Scheduled(initialDelay = 5, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void deleteExpired(){
        List<AppUser> expired = users.findByDemoTrueAndCreatedAtBefore(LocalDateTime.now().minus(LIFETIME));
        for (AppUser user : expired) {
            try {
                sessionKiller.signOutEverywhereExcept(user.getId(), null);
                accountService.deleteEverything(user);
            } catch (RuntimeException e) {
                log.warn("Couldn't delete demo account id# {}", user.getId(), e);
            }
        }
        if (!expired.isEmpty()) {
            log.info("Deleted {} expired demo accounts", expired.size());
        }
    }

    private static String randomId(int length){
        StringBuilder id = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            id.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
        }
        return id.toString();
    }
}
