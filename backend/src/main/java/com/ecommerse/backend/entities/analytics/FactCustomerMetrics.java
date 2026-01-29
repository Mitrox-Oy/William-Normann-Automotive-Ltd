package com.ecommerse.backend.entities.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Fact table storing aggregated customer metric snapshots.
 */
@Entity
@Table(name = "fact_customer_metrics")
public class FactCustomerMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Column(name = "lifetime_value", precision = 12, scale = 2)
    private BigDecimal lifetimeValue = BigDecimal.ZERO;

    @Column(name = "average_order_value", precision = 12, scale = 2)
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "last_order_date")
    private LocalDate lastOrderDate;

    @Column(name = "days_since_last_order")
    private Integer daysSinceLastOrder;

    @Column(name = "cart_abandonment_count")
    private Integer cartAbandonmentCount = 0;

    @Column(name = "first_order_date")
    private LocalDate firstOrderDate;

    @Column(name = "cohort_month", length = 7)
    private String cohortMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDate getCalculationDate() {
        return calculationDate;
    }

    public void setCalculationDate(LocalDate calculationDate) {
        this.calculationDate = calculationDate;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getLifetimeValue() {
        return lifetimeValue;
    }

    public void setLifetimeValue(BigDecimal lifetimeValue) {
        this.lifetimeValue = lifetimeValue;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(LocalDate lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public Integer getDaysSinceLastOrder() {
        return daysSinceLastOrder;
    }

    public void setDaysSinceLastOrder(Integer daysSinceLastOrder) {
        this.daysSinceLastOrder = daysSinceLastOrder;
    }

    public Integer getCartAbandonmentCount() {
        return cartAbandonmentCount;
    }

    public void setCartAbandonmentCount(Integer cartAbandonmentCount) {
        this.cartAbandonmentCount = cartAbandonmentCount;
    }

    public LocalDate getFirstOrderDate() {
        return firstOrderDate;
    }

    public void setFirstOrderDate(LocalDate firstOrderDate) {
        this.firstOrderDate = firstOrderDate;
    }

    public String getCohortMonth() {
        return cohortMonth;
    }

    public void setCohortMonth(String cohortMonth) {
        this.cohortMonth = cohortMonth;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FactCustomerMetrics that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
