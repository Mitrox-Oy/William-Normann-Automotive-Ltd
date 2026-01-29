package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Wishlist entity operations
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Find wishlist items for a specific user
     */
    Page<Wishlist> findByUserUsernameOrderByAddedDateDesc(String username, Pageable pageable);

    /**
     * Find specific wishlist item by user and product
     */
    Optional<Wishlist> findByUserUsernameAndProductId(String username, Long productId);

    /**
     * Check if user has product in wishlist
     */
    boolean existsByUserUsernameAndProductId(String username, Long productId);

    /**
     * Count wishlist items for user
     */
    long countByUserUsername(String username);

    /**
     * Delete all wishlist items for a user
     */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.username = :username")
    int deleteByUserUsername(@Param("username") String username);

    /**
     * Delete specific wishlist item
     */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.username = :username AND w.product.id = :productId")
    int deleteByUserUsernameAndProductId(@Param("username") String username, @Param("productId") Long productId);

    /**
     * Find wishlist items with product details
     */
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product p JOIN FETCH p.category WHERE w.user.username = :username AND p.active = true ORDER BY w.addedDate DESC")
    Page<Wishlist> findByUserUsernameWithProductDetails(@Param("username") String username, Pageable pageable);
}
