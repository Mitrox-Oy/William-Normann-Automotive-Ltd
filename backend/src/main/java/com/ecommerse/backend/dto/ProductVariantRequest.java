package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payload for creating or updating a product variant.
 */
@Schema(description = "Payload used to create or update a product variant")
public class ProductVariantRequest {

    @NotBlank(message = "Variant name is required")
    @Size(max = 120, message = "Variant name must be 120 characters or fewer")
    @Schema(description = "Variant name", example = "Midnight / 256 GB", required = true)
    private String name;

    @NotBlank(message = "Variant SKU is required")
    @Size(max = 60, message = "Variant SKU must be 60 characters or fewer")
    @Schema(description = "Variant SKU", example = "IPH15P-256-MID", required = true)
    private String sku;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
    @Digits(integer = 8, fraction = 2, message = "Price can have up to 8 digits and 2 decimals")
    @Schema(description = "Optional price override", example = "1049.00")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Schema(description = "Units in stock for this variant", example = "12")
    private Integer stockQuantity = 0;

    @Schema(description = "Whether the variant is active", example = "true")
    private Boolean active = Boolean.TRUE;

    @Schema(description = "Marks this variant as the default/pre-selected option", example = "false")
    private Boolean defaultVariant = Boolean.FALSE;

    @Schema(description = "Desired display order position", example = "0")
    private Integer position;

    @Schema(description = "Optional map of option key/value pairs (e.g. colour, size)")
    private Map<String, String> options = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getDefaultVariant() {
        return defaultVariant;
    }

    public void setDefaultVariant(Boolean defaultVariant) {
        this.defaultVariant = defaultVariant;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options != null ? new LinkedHashMap<>(options) : new LinkedHashMap<>();
    }
}
