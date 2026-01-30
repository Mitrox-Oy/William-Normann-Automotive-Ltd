package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Product entity operations
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        /**
         * Find all active products
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Page<Product> findByActiveTrueOrderByCreatedDateDesc(Pageable pageable);

        /**
         * Find all active products (catalog listing)
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Page<Product> findAllByActiveTrue(Pageable pageable);

        /**
         * Catalog listing with optional search/category filters.
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        @Query("SELECT DISTINCT p FROM Product p WHERE p.active = true AND "
                        + "(:categoryId IS NULL OR p.category.id = :categoryId) AND "
                        + "(:search IS NULL OR "
                        + "LOWER(p.name) LIKE CONCAT('%', :search, '%') OR "
                        + "LOWER(COALESCE(p.description, '')) LIKE CONCAT('%', :search, '%') OR "
                        + "LOWER(COALESCE(p.brand, '')) LIKE CONCAT('%', :search, '%'))")
        Page<Product> findActiveForCatalog(@Param("categoryId") Long categoryId,
                        @Param("search") String search,
                        Pageable pageable);

        /**
         * Find product by ID and active status
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Optional<Product> findByIdAndActiveTrue(Long id);

        /**
         * Find products by category
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Page<Product> findByCategoryIdAndActiveTrueOrderByCreatedDateDesc(Long categoryId, Pageable pageable);

        /**
         * Find featured products
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedDateDesc();

        /**
         * Find product by SKU (case-insensitive)
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Optional<Product> findBySkuAndActiveTrue(String sku);
        
        /**
         * Find product by SKU (case-insensitive)
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        Optional<Product> findBySkuIgnoreCaseAndActiveTrue(String sku);

        /**
         * Check if SKU exists (for unique validation)
         */
        boolean existsBySku(String sku);

        /**
         * Search products by name or description
         */
        @Query("SELECT p FROM Product p WHERE p.active = true AND " +
                        "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(p.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
        Page<Product> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

        /**
         * Find products by price range
         */
        Page<Product> findByActiveTrueAndPriceBetweenOrderByPriceAsc(
                        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

        /**
         * Find products by brand
         */
        Page<Product> findByBrandAndActiveTrueOrderByCreatedDateDesc(String brand, Pageable pageable);

        /**
         * Find low stock products
         */
        List<Product> findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(Integer threshold);

        /**
         * Get all distinct brands
         */
        @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true AND p.brand IS NOT NULL ORDER BY p.brand")
        List<String> findAllBrands();

        /**
         * Find products in specific category and its subcategories
         */
        @Query("SELECT p FROM Product p WHERE p.active = true AND " +
                        "(p.category.id = :categoryId OR p.category.parent.id = :categoryId) " +
                        "ORDER BY p.createdDate DESC")
        Page<Product> findByCategoryAndSubcategories(@Param("categoryId") Long categoryId, Pageable pageable);

        /**
         * Advanced search with multiple filters
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        @Query("SELECT DISTINCT p FROM Product p WHERE " +
                        "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
                        +
                        "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "(:brand IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) AND " +
                        "(:inStockOnly = false OR p.stockQuantity > 0) AND " +
                        "(:featuredOnly = false OR p.featured = true) AND " +
                        "p.active = true")
        Page<Product> findWithFilters(@Param("query") String query,
                        @Param("categoryId") Long categoryId,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("brand") String brand,
                        @Param("inStockOnly") Boolean inStockOnly,
                        @Param("featuredOnly") Boolean featuredOnly,
                        Pageable pageable);
}
