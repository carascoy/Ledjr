package com.Ledjr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Category;

/**
 * JpaRepository<Category, Long> gives these for free, no code needed:
 * save(category)  — insert or update
 * findById(id)    — get one row by its id
 * findAll()       — get every row
 * deleteById(id)  — delete one row
 * count()         — how many rows exist
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** All categories with a given kind (income/expense/debt), across every user. */
    List<Category> findByKind(Category.Kind kind);

    /** All categories with a given name, across every user. */
    List<Category> findByName(String name);

    /** Every category a specific user owns. */
    List<Category> findByUserId(Long userId);

    /** One user's categories of a specific kind — e.g. just their expense categories. */
    List<Category> findByUserIdAndKind(Long userId, Category.Kind kind);

    /** One user's categories, A to Z — what the category picker shows. */
    List<Category> findByUserIdOrderByNameAsc(Long userId);

    /** Does this user already have a category with this name (ignoring upper/lower case)? */
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /** How many categories this user has — used to hand out the next color slot. */
    long countByUserId(Long userId);

}
