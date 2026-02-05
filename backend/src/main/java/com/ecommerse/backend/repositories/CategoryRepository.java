package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity operations
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find all root categories (categories without parent)
     */
    List<Category> findByParentIsNullAndActiveTrue();

    /**
     * Find all children of a specific category
     */
    List<Category> findByParentIdAndActiveTrueOrderBySortOrder(Long parentId);

    /**
     * Find all children of a specific category (simple method name)
     */
    List<Category> findByParentIdAndActiveTrue(Long parentId);

    /**
     * Find category by name
     */
    Optional<Category> findByNameAndActiveTrue(String name);

    /**
     * Find category by ID and active status
     */
    Optional<Category> findByIdAndActiveTrue(Long id);

    /**
     * Find category by slug and active status
     */
    Optional<Category> findBySlugAndActiveTrue(String slug);

    /**
     * Find all active categories
     */
    List<Category> findByActiveTrueOrderBySortOrder();

    /**
     * Find all active categories with pagination
     */
    Page<Category> findByActiveTrue(Pageable pageable);

    /**
     * Search categories by name containing text
     */
    Page<Category> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Check if category name exists (for unique validation)
     */
    boolean existsByNameAndActiveTrue(String name);

    /**
     * Check if category slug exists (for unique validation)
     */
    boolean existsBySlugAndActiveTrue(String slug);

    /**
     * Find categories by level in tree (root categories = level 0)
     * For level 0, get root categories. For level 1+, we need to implement
     * differently
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true ORDER BY c.sortOrder")
    List<Category> findRootCategoriesActive();

    /**
     * Find categories with their children (tree structure)
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.active = true ORDER BY c.sortOrder")
    List<Category> findCategoryTreeRoot();

    /**
     * Find category with all its descendants
     */
    @Query("SELECT c FROM Category c WHERE c.id = :categoryId OR c.parent.id = :categoryId AND c.active = true")
    List<Category> findCategoryWithDescendants(@Param("categoryId") Long categoryId);

    /**
     * Find all active categories with sort order
     */
    List<Category> findByActiveTrueOrderBySortOrderAsc();

    /**
     * Find children by parent ID with sort order
     */
    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    /**
     * Count products in category (including subcategories)
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    Long countProductsInCategory(@Param("categoryId") Long categoryId);

    /**
     * Find category by slug (case-insensitive, for topic routing)
     */
    Optional<Category> findBySlugIgnoreCaseAndActiveTrue(String slug);

    /**
     * Find all descendants of a category (recursive via service layer)
     * This query gets immediate children; service layer handles recursion
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.active = true ORDER BY c.sortOrder")
    List<Category> findActiveDescendants(@Param("parentId") Long parentId);
}
