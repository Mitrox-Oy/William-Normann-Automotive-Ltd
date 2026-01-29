package com.ecommerse.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for cart validation results
 */
public class CartValidationResult {

    private boolean valid;
    private List<String> errors;
    private boolean cartExpired;
    private boolean cartActive;
    private Integer totalItems;
    private BigDecimal totalAmount;

    // Constructors
    public CartValidationResult() {
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean isCartExpired() {
        return cartExpired;
    }

    public void setCartExpired(boolean cartExpired) {
        this.cartExpired = cartExpired;
    }

    public boolean isCartActive() {
        return cartActive;
    }

    public void setCartActive(boolean cartActive) {
        this.cartActive = cartActive;
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

    @Override
    public String toString() {
        return "CartValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                ", cartExpired=" + cartExpired +
                ", cartActive=" + cartActive +
                ", totalItems=" + totalItems +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
