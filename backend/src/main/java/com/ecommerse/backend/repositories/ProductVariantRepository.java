package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByPositionAsc(Long productId);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    Optional<ProductVariant> findTopByProductIdOrderByPositionDesc(Long productId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    long countByProductId(Long productId);

    long countByProductIdAndDefaultVariantTrue(Long productId);

    @Query("SELECT COALESCE(SUM(v.stockQuantity),0) FROM ProductVariant v WHERE v.product.id = :productId AND v.active = true")
    Integer sumActiveStockByProductId(@Param("productId") Long productId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.defaultVariant = false WHERE v.product.id = :productId AND v.id <> :variantId")
    void clearDefaultVariantExcept(@Param("productId") Long productId, @Param("variantId") Long variantId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.defaultVariant = false WHERE v.product.id = :productId")
    void clearAllDefaultFlags(@Param("productId") Long productId);
}
