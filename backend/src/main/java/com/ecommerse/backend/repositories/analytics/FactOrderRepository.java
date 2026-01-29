package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.FactOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactOrderRepository extends JpaRepository<FactOrder, Long> {

    @Query("SELECT SUM(f.revenue) FROM FactOrder f WHERE f.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenue(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(f.quantity) FROM FactOrder f WHERE f.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalUnits(@Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);

    @Query("SELECT f.categoryId, SUM(f.revenue) FROM FactOrder f WHERE f.orderDate BETWEEN :startDate AND :endDate GROUP BY f.categoryId ORDER BY SUM(f.revenue) DESC")
    List<Object[]> getRevenueByCategory(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT f.productId, SUM(f.revenue), SUM(f.quantity) FROM FactOrder f WHERE f.orderDate BETWEEN :startDate AND :endDate GROUP BY f.productId ORDER BY SUM(f.revenue) DESC")
    List<Object[]> getRevenueByProduct(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT f.orderDate, SUM(f.revenue) FROM FactOrder f WHERE f.orderDate BETWEEN :startDate AND :endDate GROUP BY f.orderDate ORDER BY f.orderDate")
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    Optional<FactOrder> findByOrderIdAndProductId(Long orderId, Long productId);
}
