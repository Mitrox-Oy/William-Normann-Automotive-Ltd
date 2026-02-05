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
                        + "(:searchPattern IS NULL OR "
                        + "LOWER(p.name) LIKE :searchPattern OR "
                        + "LOWER(COALESCE(p.description, '')) LIKE :searchPattern OR "
                        + "LOWER(COALESCE(p.brand, '')) LIKE :searchPattern)")
        Page<Product> findActiveForCatalog(@Param("categoryId") Long categoryId,
                        @Param("searchPattern") String searchPattern,
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
                        "(LOWER(p.name) LIKE CONCAT('%', LOWER(:searchTerm), '%') OR " +
                        "LOWER(p.description) LIKE CONCAT('%', LOWER(:searchTerm), '%') OR " +
                        "LOWER(p.brand) LIKE CONCAT('%', LOWER(:searchTerm), '%'))")
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
                        "(:queryPattern IS NULL OR LOWER(p.name) LIKE :queryPattern OR LOWER(p.description) LIKE :queryPattern) AND "
                        +
                        "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "(:brandPattern IS NULL OR LOWER(p.brand) LIKE :brandPattern) AND " +
                        "(:conditionValue IS NULL OR LOWER(COALESCE(p.condition, '')) = :conditionValue) AND " +
                        "(:productTypeValue IS NULL OR LOWER(COALESCE(p.productType, '')) = :productTypeValue) AND " +
                        "(:makePattern IS NULL OR LOWER(COALESCE(p.make, '')) LIKE :makePattern) AND " +
                        "(:modelPattern IS NULL OR LOWER(COALESCE(p.model, '')) LIKE :modelPattern) AND " +
                        "(:yearMin IS NULL OR p.year >= :yearMin) AND " +
                        "(:yearMax IS NULL OR p.year <= :yearMax) AND " +
                        "(:mileageMin IS NULL OR p.mileage >= :mileageMin) AND " +
                        "(:mileageMax IS NULL OR p.mileage <= :mileageMax) AND " +
                        "(:fuelTypeValue IS NULL OR LOWER(COALESCE(p.fuelType, '')) = :fuelTypeValue) AND " +
                        "(:transmissionValue IS NULL OR LOWER(COALESCE(p.transmission, '')) = :transmissionValue) AND "
                        +
                        "(:bodyTypeValue IS NULL OR LOWER(COALESCE(p.bodyType, '')) = :bodyTypeValue) AND " +
                        "(:driveTypeValue IS NULL OR LOWER(COALESCE(p.driveType, '')) = :driveTypeValue) AND " +
                        "(:powerMin IS NULL OR p.powerKw >= :powerMin) AND " +
                        "(:powerMax IS NULL OR p.powerKw <= :powerMax) AND " +
                        "(:warrantyIncluded IS NULL OR p.warrantyIncluded = :warrantyIncluded) AND " +
                        "(:compatibilityModeValue IS NULL OR LOWER(COALESCE(p.compatibilityMode, '')) = :compatibilityModeValue) AND "
                        +
                        "(:compatibleMakePattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.compatibleMakes, ''), ',')) LIKE :compatibleMakePattern) AND "
                        +
                        "(:compatibleModelPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.compatibleModels, ''), ',')) LIKE :compatibleModelPattern) AND "
                        +
                        "(:compatibleYear IS NULL OR ((p.compatibleYearStart IS NULL OR p.compatibleYearStart <= :compatibleYear) AND (p.compatibleYearEnd IS NULL OR p.compatibleYearEnd >= :compatibleYear))) AND "
                        +
                        "(:oemTypeValue IS NULL OR LOWER(COALESCE(p.oemType, '')) = :oemTypeValue) AND " +
                        "(:partCategoryPattern IS NULL OR LOWER(COALESCE(p.partCategory, '')) LIKE :partCategoryPattern) AND "
                        +
                        "(:partNumberPattern IS NULL OR LOWER(COALESCE(p.partNumber, '')) LIKE :partNumberPattern) AND " +
                        "(:partPositionPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.partPosition, ''), ',')) LIKE :partPositionPattern) AND "
                        +
                        "(:toolCategoryPattern IS NULL OR LOWER(COALESCE(p.toolCategory, '')) LIKE :toolCategoryPattern) AND "
                        +
                        "(:powerSourceValue IS NULL OR LOWER(COALESCE(p.powerSource, '')) = :powerSourceValue) AND " +
                        "(:voltageMin IS NULL OR p.voltage >= :voltageMin) AND " +
                        "(:voltageMax IS NULL OR p.voltage <= :voltageMax) AND " +
                        "(:torqueMin IS NULL OR p.torqueMinNm >= :torqueMin) AND " +
                        "(:torqueMax IS NULL OR p.torqueMaxNm <= :torqueMax) AND " +
                        "(:driveSizeValue IS NULL OR LOWER(COALESCE(p.driveSize, '')) = :driveSizeValue) AND " +
                        "(:professionalGrade IS NULL OR p.professionalGrade = :professionalGrade) AND " +
                        "(:isKit IS NULL OR p.isKit = :isKit) AND " +
                        "(:styleTagPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.styleTags, ''), ',')) LIKE :styleTagPattern) AND "
                        +
                        "(:finishValue IS NULL OR LOWER(COALESCE(p.finish, '')) = :finishValue) AND " +
                        "(:streetLegal IS NULL OR p.streetLegal = :streetLegal) AND " +
                        "(:installationDifficultyValue IS NULL OR LOWER(COALESCE(p.installationDifficulty, '')) = :installationDifficultyValue) AND "
                        +
                        "(:customCategoryPattern IS NULL OR LOWER(COALESCE(p.customCategory, '')) LIKE :customCategoryPattern) AND "
                        +
                        "(:inStockOnly = false OR p.stockQuantity > 0) AND " +
                        "(:featuredOnly = false OR p.featured = true) AND " +
                        "p.active = true")
        Page<Product> findWithFilters(@Param("queryPattern") String queryPattern,
                        @Param("categoryId") Long categoryId,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("brandPattern") String brandPattern,
                        @Param("conditionValue") String conditionValue,
                        @Param("productTypeValue") String productTypeValue,
                        @Param("makePattern") String makePattern,
                        @Param("modelPattern") String modelPattern,
                        @Param("yearMin") Integer yearMin,
                        @Param("yearMax") Integer yearMax,
                        @Param("mileageMin") Integer mileageMin,
                        @Param("mileageMax") Integer mileageMax,
                        @Param("fuelTypeValue") String fuelTypeValue,
                        @Param("transmissionValue") String transmissionValue,
                        @Param("bodyTypeValue") String bodyTypeValue,
                        @Param("driveTypeValue") String driveTypeValue,
                        @Param("powerMin") Integer powerMin,
                        @Param("powerMax") Integer powerMax,
                        @Param("warrantyIncluded") Boolean warrantyIncluded,
                        @Param("compatibilityModeValue") String compatibilityModeValue,
                        @Param("compatibleMakePattern") String compatibleMakePattern,
                        @Param("compatibleModelPattern") String compatibleModelPattern,
                        @Param("compatibleYear") Integer compatibleYear,
                        @Param("oemTypeValue") String oemTypeValue,
                        @Param("partCategoryPattern") String partCategoryPattern,
                        @Param("partNumberPattern") String partNumberPattern,
                        @Param("partPositionPattern") String partPositionPattern,
                        @Param("toolCategoryPattern") String toolCategoryPattern,
                        @Param("powerSourceValue") String powerSourceValue,
                        @Param("voltageMin") Integer voltageMin,
                        @Param("voltageMax") Integer voltageMax,
                        @Param("torqueMin") Integer torqueMin,
                        @Param("torqueMax") Integer torqueMax,
                        @Param("driveSizeValue") String driveSizeValue,
                        @Param("professionalGrade") Boolean professionalGrade,
                        @Param("isKit") Boolean isKit,
                        @Param("styleTagPattern") String styleTagPattern,
                        @Param("finishValue") String finishValue,
                        @Param("streetLegal") Boolean streetLegal,
                        @Param("installationDifficultyValue") String installationDifficultyValue,
                        @Param("customCategoryPattern") String customCategoryPattern,
                        @Param("inStockOnly") Boolean inStockOnly,
                        @Param("featuredOnly") Boolean featuredOnly,
                        Pageable pageable);

        /**
         * Find products by category IDs (for root category scoping)
         * Used when filtering by a root category and all its descendants
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        @Query("SELECT DISTINCT p FROM Product p WHERE p.active = true AND " +
                        "p.category.id IN :categoryIds AND " +
                        "(:searchPattern IS NULL OR " +
                        "LOWER(p.name) LIKE :searchPattern OR " +
                        "LOWER(COALESCE(p.description, '')) LIKE :searchPattern OR " +
                        "LOWER(COALESCE(p.brand, '')) LIKE :searchPattern)")
        Page<Product> findByCategoryIdsAndSearch(@Param("categoryIds") List<Long> categoryIds,
                        @Param("searchPattern") String searchPattern,
                        Pageable pageable);

        /**
         * Advanced search with root category scope (products within a root category's
         * subtree)
         */
        @EntityGraph(attributePaths = { "images", "variants" })
        @Query("SELECT DISTINCT p FROM Product p WHERE p.active = true AND " +
                        "p.category.id IN :categoryIds AND " +
                        "(:queryPattern IS NULL OR LOWER(p.name) LIKE :queryPattern OR LOWER(p.description) LIKE :queryPattern) AND "
                        +
                        "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "(:brandPattern IS NULL OR LOWER(p.brand) LIKE :brandPattern) AND " +
                        "(:conditionValue IS NULL OR LOWER(COALESCE(p.condition, '')) = :conditionValue) AND " +
                        "(:productTypeValue IS NULL OR LOWER(COALESCE(p.productType, '')) = :productTypeValue) AND " +
                        "(:makePattern IS NULL OR LOWER(COALESCE(p.make, '')) LIKE :makePattern) AND " +
                        "(:modelPattern IS NULL OR LOWER(COALESCE(p.model, '')) LIKE :modelPattern) AND " +
                        "(:yearMin IS NULL OR p.year >= :yearMin) AND " +
                        "(:yearMax IS NULL OR p.year <= :yearMax) AND " +
                        "(:mileageMin IS NULL OR p.mileage >= :mileageMin) AND " +
                        "(:mileageMax IS NULL OR p.mileage <= :mileageMax) AND " +
                        "(:fuelTypeValue IS NULL OR LOWER(COALESCE(p.fuelType, '')) = :fuelTypeValue) AND " +
                        "(:transmissionValue IS NULL OR LOWER(COALESCE(p.transmission, '')) = :transmissionValue) AND "
                        +
                        "(:bodyTypeValue IS NULL OR LOWER(COALESCE(p.bodyType, '')) = :bodyTypeValue) AND " +
                        "(:driveTypeValue IS NULL OR LOWER(COALESCE(p.driveType, '')) = :driveTypeValue) AND " +
                        "(:powerMin IS NULL OR p.powerKw >= :powerMin) AND " +
                        "(:powerMax IS NULL OR p.powerKw <= :powerMax) AND " +
                        "(:warrantyIncluded IS NULL OR p.warrantyIncluded = :warrantyIncluded) AND " +
                        "(:compatibilityModeValue IS NULL OR LOWER(COALESCE(p.compatibilityMode, '')) = :compatibilityModeValue) AND "
                        +
                        "(:compatibleMakePattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.compatibleMakes, ''), ',')) LIKE :compatibleMakePattern) AND "
                        +
                        "(:compatibleModelPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.compatibleModels, ''), ',')) LIKE :compatibleModelPattern) AND "
                        +
                        "(:compatibleYear IS NULL OR ((p.compatibleYearStart IS NULL OR p.compatibleYearStart <= :compatibleYear) AND (p.compatibleYearEnd IS NULL OR p.compatibleYearEnd >= :compatibleYear))) AND "
                        +
                        "(:oemTypeValue IS NULL OR LOWER(COALESCE(p.oemType, '')) = :oemTypeValue) AND " +
                        "(:partCategoryPattern IS NULL OR LOWER(COALESCE(p.partCategory, '')) LIKE :partCategoryPattern) AND "
                        +
                        "(:partNumberPattern IS NULL OR LOWER(COALESCE(p.partNumber, '')) LIKE :partNumberPattern) AND " +
                        "(:partPositionPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.partPosition, ''), ',')) LIKE :partPositionPattern) AND "
                        +
                        "(:toolCategoryPattern IS NULL OR LOWER(COALESCE(p.toolCategory, '')) LIKE :toolCategoryPattern) AND "
                        +
                        "(:powerSourceValue IS NULL OR LOWER(COALESCE(p.powerSource, '')) = :powerSourceValue) AND " +
                        "(:voltageMin IS NULL OR p.voltage >= :voltageMin) AND " +
                        "(:voltageMax IS NULL OR p.voltage <= :voltageMax) AND " +
                        "(:torqueMin IS NULL OR p.torqueMinNm >= :torqueMin) AND " +
                        "(:torqueMax IS NULL OR p.torqueMaxNm <= :torqueMax) AND " +
                        "(:driveSizeValue IS NULL OR LOWER(COALESCE(p.driveSize, '')) = :driveSizeValue) AND " +
                        "(:professionalGrade IS NULL OR p.professionalGrade = :professionalGrade) AND " +
                        "(:isKit IS NULL OR p.isKit = :isKit) AND " +
                        "(:styleTagPattern IS NULL OR LOWER(CONCAT(',', COALESCE(p.styleTags, ''), ',')) LIKE :styleTagPattern) AND "
                        +
                        "(:finishValue IS NULL OR LOWER(COALESCE(p.finish, '')) = :finishValue) AND " +
                        "(:streetLegal IS NULL OR p.streetLegal = :streetLegal) AND " +
                        "(:installationDifficultyValue IS NULL OR LOWER(COALESCE(p.installationDifficulty, '')) = :installationDifficultyValue) AND "
                        +
                        "(:customCategoryPattern IS NULL OR LOWER(COALESCE(p.customCategory, '')) LIKE :customCategoryPattern) AND "
                        +
                        "(:inStockOnly = false OR p.stockQuantity > 0) AND " +
                        "(:featuredOnly = false OR p.featured = true)")
        Page<Product> findWithFiltersAndRootScope(@Param("categoryIds") List<Long> categoryIds,
                        @Param("queryPattern") String queryPattern,
                        @Param("categoryId") Long categoryId,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("brandPattern") String brandPattern,
                        @Param("conditionValue") String conditionValue,
                        @Param("productTypeValue") String productTypeValue,
                        @Param("makePattern") String makePattern,
                        @Param("modelPattern") String modelPattern,
                        @Param("yearMin") Integer yearMin,
                        @Param("yearMax") Integer yearMax,
                        @Param("mileageMin") Integer mileageMin,
                        @Param("mileageMax") Integer mileageMax,
                        @Param("fuelTypeValue") String fuelTypeValue,
                        @Param("transmissionValue") String transmissionValue,
                        @Param("bodyTypeValue") String bodyTypeValue,
                        @Param("driveTypeValue") String driveTypeValue,
                        @Param("powerMin") Integer powerMin,
                        @Param("powerMax") Integer powerMax,
                        @Param("warrantyIncluded") Boolean warrantyIncluded,
                        @Param("compatibilityModeValue") String compatibilityModeValue,
                        @Param("compatibleMakePattern") String compatibleMakePattern,
                        @Param("compatibleModelPattern") String compatibleModelPattern,
                        @Param("compatibleYear") Integer compatibleYear,
                        @Param("oemTypeValue") String oemTypeValue,
                        @Param("partCategoryPattern") String partCategoryPattern,
                        @Param("partNumberPattern") String partNumberPattern,
                        @Param("partPositionPattern") String partPositionPattern,
                        @Param("toolCategoryPattern") String toolCategoryPattern,
                        @Param("powerSourceValue") String powerSourceValue,
                        @Param("voltageMin") Integer voltageMin,
                        @Param("voltageMax") Integer voltageMax,
                        @Param("torqueMin") Integer torqueMin,
                        @Param("torqueMax") Integer torqueMax,
                        @Param("driveSizeValue") String driveSizeValue,
                        @Param("professionalGrade") Boolean professionalGrade,
                        @Param("isKit") Boolean isKit,
                        @Param("styleTagPattern") String styleTagPattern,
                        @Param("finishValue") String finishValue,
                        @Param("streetLegal") Boolean streetLegal,
                        @Param("installationDifficultyValue") String installationDifficultyValue,
                        @Param("customCategoryPattern") String customCategoryPattern,
                        @Param("inStockOnly") Boolean inStockOnly,
                        @Param("featuredOnly") Boolean featuredOnly,
                        Pageable pageable);

        /**
         * Get distinct brands for products within specific category IDs
         * Used for topic-scoped brand filtering
         */
        @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true AND p.brand IS NOT NULL AND p.category.id IN :categoryIds ORDER BY p.brand")
        List<String> findBrandsByCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}
