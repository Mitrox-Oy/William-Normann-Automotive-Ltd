package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CartItem entity operations
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find cart items by cart ID
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Find cart item by cart ID and product ID
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Find cart items by user ID (through cart relationship)
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") Long userId);

    /**
     * Count total items in cart
     */
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countByCartId(@Param("cartId") Long cartId);

    /**
     * Sum total quantity in cart
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") Long cartId);

    /**
     * Delete all cart items by cart ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    /**
     * Delete cart items by product ID (when product is deleted)
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Find cart items with insufficient stock
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.quantity > ci.product.stockQuantity")
    List<CartItem> findItemsWithInsufficientStock();

    /**
     * Find cart items with inactive products
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.product.active = false")
    List<CartItem> findItemsWithInactiveProducts();

    /**
     * Find reservations that have expired.
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.reservedUntil IS NOT NULL AND ci.reservedUntil < :threshold")
    List<CartItem> findExpiredReservations(@Param("threshold") LocalDateTime threshold);
}
