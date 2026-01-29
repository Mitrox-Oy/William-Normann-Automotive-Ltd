package com.ecommerse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for cart analytics data
 */
public class CartAnalytics {

    private Long cartId;
    private LocalDateTime createdDate;
    private LocalDateTime lastActivity;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private Integer uniqueProducts;
    private BigDecimal averageItemPrice;
    private boolean isExpired;
    private boolean isActive;

    // Constructors
    public CartAnalytics() {
    }

    // Getters and Setters
    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getUniqueProducts() {
        return uniqueProducts;
    }

    public void setUniqueProducts(Integer uniqueProducts) {
        this.uniqueProducts = uniqueProducts;
    }

    public BigDecimal getAverageItemPrice() {
        return averageItemPrice;
    }

    public void setAverageItemPrice(BigDecimal averageItemPrice) {
        this.averageItemPrice = averageItemPrice;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setIsExpired(boolean isExpired) {
        this.isExpired = isExpired;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "CartAnalytics{" +
                "cartId=" + cartId +
                ", createdDate=" + createdDate +
                ", lastActivity=" + lastActivity +
                ", totalItems=" + totalItems +
                ", totalAmount=" + totalAmount +
                ", uniqueProducts=" + uniqueProducts +
                ", averageItemPrice=" + averageItemPrice +
                ", isExpired=" + isExpired +
                ", isActive=" + isActive +
                '}';
    }
}
