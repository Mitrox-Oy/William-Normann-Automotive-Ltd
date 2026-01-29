package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Order
 */
@Schema(description = "Order information")
public class OrderDTO {

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Order number", example = "ORD-1690123456789")
    private String orderNumber;

    @Schema(description = "User ID who placed the order", example = "1")
    private Long userId;

    @Schema(description = "Username of order placer", example = "john@example.com")
    private String username;

    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Total amount including shipping and tax", example = "1149.97")
    private BigDecimal totalAmount;

    @Schema(description = "Shipping amount", example = "9.99")
    private BigDecimal shippingAmount;

    @Schema(description = "Tax amount", example = "139.99")
    private BigDecimal taxAmount;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255, message = "Shipping address cannot exceed 255 characters")
    @Schema(description = "Shipping address", example = "123 Main St, Apt 4B", required = true)
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    @Size(max = 100, message = "Shipping city cannot exceed 100 characters")
    @Schema(description = "Shipping city", example = "New York", required = true)
    private String shippingCity;

    @NotBlank(message = "Shipping postal code is required")
    @Size(max = 20, message = "Shipping postal code cannot exceed 20 characters")
    @Schema(description = "Shipping postal code", example = "10001", required = true)
    private String shippingPostalCode;

    @NotBlank(message = "Shipping country is required")
    @Size(max = 100, message = "Shipping country cannot exceed 100 characters")
    @Schema(description = "Shipping country", example = "United States", required = true)
    private String shippingCountry;

    @Schema(description = "Items in the order")
    private List<OrderItemDTO> orderItems;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Schema(description = "Order notes", example = "Please deliver after 6 PM")
    private String notes;

    @Schema(description = "Order placement date")
    private LocalDateTime orderDate;

    @Schema(description = "Order shipped date")
    private LocalDateTime shippedDate;

    @Schema(description = "Order delivered date")
    private LocalDateTime deliveredDate;

    @Schema(description = "Whether order can be cancelled", example = "true")
    private Boolean canBeCancelled;

    @Schema(description = "Stripe Checkout session identifier", example = "cs_test_a1b2c3")
    private String stripeCheckoutSessionId;

    @Schema(description = "Stripe payment intent identifier", example = "pi_test_a1b2c3")
    private String paymentIntentId;

    @Schema(description = "Indicates if inventory is reserved for this order", example = "true")
    private Boolean inventoryLocked;

    @Schema(description = "Whether order can be shipped", example = "false")
    private Boolean canBeShipped;

    @Schema(description = "Whether order can be delivered", example = "false")
    private Boolean canBeDelivered;

    @Schema(description = "Order creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Order last update date")
    private LocalDateTime updatedDate;

    // Constructors
    public OrderDTO() {
    }

    public OrderDTO(Long id, String orderNumber, OrderStatus status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDateTime getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(LocalDateTime shippedDate) {
        this.shippedDate = shippedDate;
    }

    public LocalDateTime getDeliveredDate() {
        return deliveredDate;
    }

    public void setDeliveredDate(LocalDateTime deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public Boolean getCanBeCancelled() {
        return canBeCancelled;
    }

    public void setCanBeCancelled(Boolean canBeCancelled) {
        this.canBeCancelled = canBeCancelled;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public Boolean getInventoryLocked() {
        return inventoryLocked;
    }

    public void setInventoryLocked(Boolean inventoryLocked) {
        this.inventoryLocked = inventoryLocked;
    }

    public Boolean getCanBeShipped() {
        return canBeShipped;
    }

    public void setCanBeShipped(Boolean canBeShipped) {
        this.canBeShipped = canBeShipped;
    }

    public Boolean getCanBeDelivered() {
        return canBeDelivered;
    }

    public void setCanBeDelivered(Boolean canBeDelivered) {
        this.canBeDelivered = canBeDelivered;
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
