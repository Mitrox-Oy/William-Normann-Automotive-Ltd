package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Order Item
 */
@Schema(description = "Order item information")
public class OrderItemDTO {

    @Schema(description = "Order item ID", example = "1")
    private Long id;

    @Schema(description = "Order ID", example = "1")
    private Long orderId;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name at time of order", example = "iPhone 15 Pro")
    private String productName;

    @Schema(description = "Product SKU at time of order", example = "IPH15P-256-BLK")
    private String productSku;

    @Schema(description = "Quantity ordered", example = "1")
    private Integer quantity;

    @Schema(description = "Unit price at time of order", example = "999.99")
    private BigDecimal unitPrice;

    @Schema(description = "Total price for this item (unitPrice * quantity)", example = "999.99")
    private BigDecimal totalPrice;

    @Schema(description = "Item creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Item last update date")
    private LocalDateTime updatedDate;

    // Constructors
    public OrderItemDTO() {
    }

    public OrderItemDTO(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

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

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
