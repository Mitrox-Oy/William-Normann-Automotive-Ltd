package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating an order
 */
@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

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

    @Schema(description = "Shipping amount", example = "9.99")
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Schema(description = "Tax amount", example = "139.99")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Schema(description = "Order notes", example = "Please deliver after 6 PM")
    private String notes;

    // Constructors
    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String shippingAddress, String shippingCity,
            String shippingPostalCode, String shippingCountry) {
        this.shippingAddress = shippingAddress;
        this.shippingCity = shippingCity;
        this.shippingPostalCode = shippingPostalCode;
        this.shippingCountry = shippingCountry;
    }

    // Getters and Setters
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
