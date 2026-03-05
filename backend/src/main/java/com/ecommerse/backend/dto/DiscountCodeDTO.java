package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Discount code configuration")
public class DiscountCodeDTO {

    @Schema(description = "Discount ID", example = "1")
    private Long id;

    @NotBlank(message = "Code is required")
    @Size(max = 64, message = "Code must be at most 64 characters")
    @Schema(description = "Discount code entered by customer", example = "SAVE10")
    private String code;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Schema(description = "Admin description", example = "10% off selected categories")
    private String description;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.01", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percentage must be 100 or less")
    @Schema(description = "Percentage discount value", example = "10.00")
    private BigDecimal percentage;

    @Schema(description = "Whether this code is active", example = "true")
    private Boolean active;

    @Schema(description = "If true, discount applies to all products", example = "true")
    private Boolean appliesToAllProducts;

    @Schema(description = "Category IDs this code applies to when appliesToAllProducts=false")
    private List<Long> categoryIds;

    @Schema(description = "Category names for convenience")
    private List<String> categoryNames;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdDate;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getAppliesToAllProducts() {
        return appliesToAllProducts;
    }

    public void setAppliesToAllProducts(Boolean appliesToAllProducts) {
        this.appliesToAllProducts = appliesToAllProducts;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public void setCategoryNames(List<String> categoryNames) {
        this.categoryNames = categoryNames;
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
