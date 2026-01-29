package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Cart Item
 */
@Schema(description = "Cart item information")
public class CartItemDTO {

    @Schema(description = "Cart item ID", example = "1")
    private Long id;

    @Schema(description = "Cart ID", example = "1")
    private Long cartId;

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "1", required = true)
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 15 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPH15P-256-BLK")
    private String productSku;

    @Schema(description = "Product image URL", example = "https://example.com/images/iphone15pro.jpg")
    private String productImageUrl;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the item", example = "2", required = true)
    private Integer quantity;

    @Schema(description = "Unit price at the time of adding to cart", example = "999.99")
    private BigDecimal unitPrice;

    @Schema(description = "Total price for this item (unitPrice * quantity)", example = "1999.98")
    private BigDecimal totalPrice;

    @Schema(description = "Whether the product is currently available", example = "true")
    private Boolean available;

    @Schema(description = "Whether there is sufficient stock for this quantity", example = "true")
    private Boolean inStock;

    @Schema(description = "Current stock quantity of the product", example = "48")
    private Integer currentStock;

    @Schema(description = "Item creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Item last update date")
    private LocalDateTime updatedDate;

    @Schema(description = "Reservation expiration timestamp")
    private LocalDateTime reservationExpiresAt;

    // Constructors
    public CartItemDTO() {
    }

    public CartItemDTO(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
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

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
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

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
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

    public LocalDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }
}
