package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity operations
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find orders by user
     */
    Page<Order> findByUserOrderByCreatedDateDesc(User user, Pageable pageable);

    /**
     * Find orders by user ID
     */
    Page<Order> findByUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);

    /**
     * Find order by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find orders by status
     */
    Page<Order> findByStatusOrderByCreatedDateDesc(OrderStatus status, Pageable pageable);

    /**
     * Find orders by user and status
     */
    Page<Order> findByUserAndStatusOrderByCreatedDateDesc(User user, OrderStatus status, Pageable pageable);

    /**
     * Find orders within date range
     */
    Page<Order> findByCreatedDateBetweenOrderByCreatedDateDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find orders with total amount greater than specified value
     */
    List<Order> findByTotalAmountGreaterThanEqualOrderByCreatedDateDesc(BigDecimal minAmount);

    /**
     * Get orders count by status
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);

    /**
     * Get total sales amount
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal getTotalSalesByStatus(@Param("status") OrderStatus status);

    /**
     * Find orders that need to be shipped (confirmed status)
     */
    List<Order> findByStatusOrderByCreatedDateAsc(OrderStatus status);

    /**
     * Find recent orders for a user
     */
    List<Order> findTop5ByUserOrderByCreatedDateDesc(User user);

    /**
     * Find order with items by order ID
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    List<Order> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    Long countByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find orders by order number containing search term (case-insensitive)
     */
    @Query("SELECT o FROM Order o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Order> findByOrderNumberContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count orders for a status that have a Stripe payment intent attached.
     */
    Long countByStatusAndPaymentIntentIdIsNotNull(OrderStatus status);

    /**
     * Sum total sales for orders with Stripe payment intents for a specific status.
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.paymentIntentId IS NOT NULL")
    BigDecimal getTotalSalesByStatusAndPaymentIntent(@Param("status") OrderStatus status);

    /**
     * Fetch Stripe-backed orders for a status ordered by most recent first.
     */
    Page<Order> findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(OrderStatus status, Pageable pageable);

    Optional<Order> findByStripeCheckoutSessionId(String sessionId);

    Optional<Order> findTopByUserOrderByCreatedDateDesc(User user);

    /**
     * Count orders for a specific user
     */
    Long countByUser(User user);

    /**
     * Sum total amount for a specific user
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user = :user")
    BigDecimal sumTotalAmountByUser(@Param("user") User user);

    /**
     * Get last order date for a user
     */
    @Query("SELECT MAX(o.createdDate) FROM Order o WHERE o.user = :user")
    LocalDateTime findLastOrderDateByUser(@Param("user") User user);
}
