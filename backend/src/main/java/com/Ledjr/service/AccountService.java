package com.Ledjr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.Ledjr.entity.AppUser;
import com.Ledjr.repository.AppUserRepository;
import com.Ledjr.repository.BudgetRepository;
import com.Ledjr.repository.CategoryRepository;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;
import com.Ledjr.repository.UnallocatedFundsRepository;

/** deleting an account and everything in it */
@Service
public class AccountService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTemplateRepository recurringRepository;
    private final DebtRepository debtRepository;
    private final CategoryRepository categoryRepository;
    private final UnallocatedFundsRepository unallocatedFundsRepository;
    private final AppUserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    public AccountService(TransactionRepository transactionRepository, BudgetRepository budgetRepository,
                          RecurringTemplateRepository recurringRepository, DebtRepository debtRepository,
                          CategoryRepository categoryRepository, UnallocatedFundsRepository unallocatedFundsRepository,
                          AppUserRepository userRepository){
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.recurringRepository = recurringRepository;
        this.debtRepository = debtRepository;
        this.categoryRepository = categoryRepository;
        this.unallocatedFundsRepository = unallocatedFundsRepository;
        this.userRepository = userRepository;
    }

    /**
     * wipes everything. order matters because of the foreign keys: transactions point at
     * categories/debts/recurring, budgets point at categories, so those go first and the user goes last.
     * flush after each step so postgres sees the deletes in this order.
     * all one transaction: if anything fails nothing is deleted
     */
    @Transactional
    public void deleteEverything(AppUser user){
        Long id = user.getId();
        transactionRepository.deleteAll(transactionRepository.findByUserId(id));
        transactionRepository.flush();
        budgetRepository.deleteAll(budgetRepository.findByUserId(id));
        budgetRepository.flush();
        recurringRepository.deleteAll(recurringRepository.findByUserId(id));
        recurringRepository.flush();
        debtRepository.deleteAll(debtRepository.findByUserId(id));
        debtRepository.flush();
        categoryRepository.deleteAll(categoryRepository.findByUserId(id));
        categoryRepository.flush();
        unallocatedFundsRepository.findByUserId(id).ifPresent(unallocatedFundsRepository::delete);
        userRepository.delete(user);
        log.info("User id# {} deleted their account", id);
    }
}
