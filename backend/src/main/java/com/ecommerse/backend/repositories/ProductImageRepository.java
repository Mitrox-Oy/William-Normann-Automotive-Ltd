package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);

    Optional<ProductImage> findByProductIdAndIsMainTrue(Long productId);

    @Query("SELECT COUNT(pi) FROM ProductImage pi WHERE pi.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isMain = false WHERE pi.product.id = :productId")
    void clearMainImageForProduct(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.position = pi.position + 1 WHERE pi.product.id = :productId AND pi.position >= :position")
    void incrementPositionsFromPosition(@Param("productId") Long productId, @Param("position") Integer position);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.position = pi.position - 1 WHERE pi.product.id = :productId AND pi.position > :position")
    void decrementPositionsAfterPosition(@Param("productId") Long productId, @Param("position") Integer position);

    boolean existsByProductIdAndPosition(Long productId, Integer position);

    void deleteByProductIdAndId(Long productId, Long imageId);
}
