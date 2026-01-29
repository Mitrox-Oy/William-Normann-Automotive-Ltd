package com.ecommerse.backend.dto.analytics;

import java.math.BigDecimal;

/**
 * Represents a ranked product performance record for dashboards.
 */
public class ProductPerformanceDTO {

    private Long productId;
    private String productName;
    private BigDecimal revenue;
    private Long quantity;
    private Double growthRate;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }
}
