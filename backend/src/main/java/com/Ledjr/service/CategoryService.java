package com.Ledjr.service;

import java.math.BigDecimal;
import java.util.List;

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

    public void newCategory(AppUser user, String name, Category.Kind kind, BigDecimal budgetedAmount){
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setKind(kind);
        category.setBudgetedAmount(budgetedAmount);
        categoryRepository.save(category);
        log.info("User id# {} created a new category called {}", category.getUser().getId(), category.getName());
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
