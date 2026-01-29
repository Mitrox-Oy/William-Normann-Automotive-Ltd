package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Cart
 */
@Schema(description = "Shopping cart information")
public class CartDTO {

    @Schema(description = "Cart ID", example = "1")
    private Long id;

    @Schema(description = "User ID who owns the cart", example = "1")
    private Long userId;

    @Schema(description = "Username of cart owner", example = "john@example.com")
    private String username;

    @Schema(description = "Items in the cart")
    private List<CartItemDTO> items;

    @Schema(description = "Total amount of all items in cart", example = "299.99")
    private BigDecimal totalAmount;

    @Schema(description = "Total number of different items in cart", example = "3")
    private Integer itemCount;

    @Schema(description = "Total quantity of all items in cart", example = "5")
    private Integer totalQuantity;

    @Schema(description = "Total number of items in cart", example = "3")
    private Integer totalItems;

    @Schema(description = "Whether the cart is empty", example = "false")
    private Boolean isEmpty;

    @Schema(description = "Cart creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Cart last update date")
    private LocalDateTime updatedDate;

    @Schema(description = "Cart expiration date")
    private LocalDateTime expiresAt;

    @Schema(description = "Whether the cart is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Cart session ID")
    private String sessionId;

    @Schema(description = "Last activity timestamp")
    private LocalDateTime lastActivity;

    @Schema(description = "Whether the cart is expired", example = "false")
    private Boolean isExpired;

    @Schema(description = "Whether the cart is valid", example = "true")
    private Boolean isValid;

    // Constructors
    public CartDTO() {
    }

    public CartDTO(Long id, Long userId, String username) {
        this.id = id;
        this.userId = userId;
        this.username = username;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Boolean getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(Boolean isEmpty) {
        this.isEmpty = isEmpty;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
}
