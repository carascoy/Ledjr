package com.Ledjr.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.BudgetRepository;
import com.Ledjr.repository.CategoryRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTemplateRepository recurringRepository;
    private final BudgetRepository budgetRepository;
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                           RecurringTemplateRepository recurringRepository, BudgetRepository budgetRepository){
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.recurringRepository = recurringRepository;
        this.budgetRepository = budgetRepository;
    }

    /** 8 tag colors on the frontend, slots go 0, 1, 2... and wrap after 7 */
    private static final int COLOR_SLOTS = 8;

    /** starter categories so the picker isn't empty on day one */
    private static final List<String> DEFAULT_EXPENSES = List.of(
        "Groceries", "Rent", "Dining Out", "Transportation", "Utilities", "Shopping", "Entertainment");
    private static final List<String> DEFAULT_INCOME = List.of("Paycheck");

    /** makes the category + gives it the next color slot */
    public Category create(AppUser user, String name, Category.Kind kind){
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setKind(kind);
        category.setColorSlot((int) (categoryRepository.countByUserId(user.getId()) % COLOR_SLOTS));
        categoryRepository.save(category);
        log.info("User id# {} created a new category called {}", user.getId(), name);
        return category;
    }

    /** rename and/or recolor. null = leave that one alone */
    public Category update(Category category, String name, Integer colorSlot){
        if (name != null) category.setName(name);
        if (colorSlot != null) category.setColorSlot(colorSlot);
        return categoryRepository.save(category);
    }

    /**
     * deletes the category but no money data: its transactions + recurring bills just become
     * uncategorized. its budgets do go, a budget for a category that doesn't exist means nothing
     */
    @Transactional
    public void delete(Category category){
        for (Transaction t : transactionRepository.findByCategoryId(category.getId())) {
            t.setCategory(null);
            transactionRepository.save(t);
        }
        for (RecurringTemplate r : recurringRepository.findByCategoryId(category.getId())) {
            r.setCategory(null);
            recurringRepository.save(r);
        }
        budgetRepository.deleteAll(budgetRepository.findByCategoryId(category.getId()));
        categoryRepository.delete(category);
        log.info("User id# {} deleted category {}", category.getUser().getId(), category.getName());
    }

    public boolean nameTakenByAnother(Long userId, String name, Long categoryId){
        return categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, categoryId);
    }

    /** only adds them if the user has none yet, so it's safe to call twice */
    public void seedDefaults(AppUser user){
        if (categoryRepository.countByUserId(user.getId()) > 0) {
            return;
        }
        for (String name : DEFAULT_EXPENSES) {
            create(user, name, Category.Kind.expense);
        }
        for (String name : DEFAULT_INCOME) {
            create(user, name, Category.Kind.income);
        }
    }

    public Optional<Category> findById(Long id){
        if (id == null) {
            return Optional.empty();
        }
        return categoryRepository.findById(id);
    }

    public List<Category> findByUserIdSorted(Long userId){
        return categoryRepository.findByUserIdOrderByNameAsc(userId);
    }

    public boolean nameTaken(Long userId, String name){
        return categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name);
    }

    public List<Category> findByKind(Category.Kind kind){
        return categoryRepository.findByKind(kind);
    }

    public List<Category> findByName(String name){
        return categoryRepository.findByName(name);
    }

    public List<Category> findByUserId(Long userid){
        return categoryRepository.findByUserId(userid);
    }

}
