package com.Ledjr.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.repository.CategoryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    /** The 8 tag colors on the frontend; slots are handed out 0, 1, 2 ... and wrap after 7. */
    private static final int COLOR_SLOTS = 8;

    /** Starter categories every new user gets, so the picker isn't empty on day one. */
    private static final List<String> DEFAULT_EXPENSES = List.of(
        "Groceries", "Rent", "Dining Out", "Transportation", "Utilities", "Shopping", "Entertainment");
    private static final List<String> DEFAULT_INCOME = List.of("Paycheck");

    /** Creates a category and gives it the next color slot. */
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

    /** Adds the starter categories, but only if the user has none yet (safe to call twice). */
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
