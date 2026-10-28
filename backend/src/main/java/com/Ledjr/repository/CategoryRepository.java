package com.Ledjr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** careful: across ALL users */
    List<Category> findByKind(Category.Kind kind);

    /** careful: across ALL users */
    List<Category> findByName(String name);

    List<Category> findByUserId(Long userId);

    /** e.g. just someone's expense categories */
    List<Category> findByUserIdAndKind(Long userId, Category.Kind kind);

    /** A to Z, what the category picker shows */
    List<Category> findByUserIdOrderByNameAsc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /** same check but skipping one category, for renaming it (renaming Rent to rent is fine) */
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    /** used to pick the next color slot */
    long countByUserId(Long userId);

}
