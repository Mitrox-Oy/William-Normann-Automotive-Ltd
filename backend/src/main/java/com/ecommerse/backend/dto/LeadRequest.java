package com.ecommerse.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LeadRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 160, message = "Email cannot exceed 160 characters")
    private String email;

    @Size(max = 60, message = "Phone cannot exceed 60 characters")
    private String phone;

    @Size(max = 120, message = "Interest cannot exceed 120 characters")
    private String interest;

    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message cannot exceed 4000 characters")
    private String message;

    private Boolean consent;

    @NotBlank(message = "Source is required")
    @Size(max = 80, message = "Source cannot exceed 80 characters")
    private String source;

    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;

    @Size(max = 200, message = "Product cannot exceed 200 characters")
    private String product;

    @Size(max = 120, message = "Part number cannot exceed 120 characters")
    private String partNumber;

    @Size(max = 30, message = "Quantity cannot exceed 30 characters")
    private String quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getConsent() {
        return consent;
    }

    public void setConsent(Boolean consent) {
        this.consent = consent;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}