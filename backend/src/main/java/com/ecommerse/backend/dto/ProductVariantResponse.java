package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.ProductVariant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO representation of {@link ProductVariant} for catalog responses.
 */
@Schema(description = "Product variant information")
public class ProductVariantResponse {

    @Schema(description = "Variant identifier", example = "1001")
    private Long id;

    @Schema(description = "Variant name shown to customers", example = "Midnight Black / 128 GB")
    private String name;

    @Schema(description = "Variant SKU code", example = "IPH15P-128-MBK")
    private String sku;

    @Schema(description = "Variant specific price override", example = "1049.00")
    private BigDecimal price;

    @Schema(description = "Units in stock for this variant", example = "12")
    private Integer stockQuantity;

    @Schema(description = "Whether the variant is currently active", example = "true")
    private Boolean active;

    @Schema(description = "Marks the default/pre-selected variant", example = "true")
    private Boolean defaultVariant;

    @Schema(description = "Display order position", example = "0")
    private Integer position;

    @Schema(description = "Key/value map describing option selections (e.g. colour/size)")
    private Map<String, String> options = new LinkedHashMap<>();

    @Schema(description = "Variant creation timestamp")
    private LocalDateTime createdDate;

    @Schema(description = "Variant last update timestamp")
    private LocalDateTime updatedDate;

    public ProductVariantResponse() {
        // default constructor
    }

    public ProductVariantResponse(ProductVariant variant) {
        this.id = variant.getId();
        this.name = variant.getName();
        this.sku = variant.getSku();
        this.price = variant.getPrice();
        this.stockQuantity = variant.getStockQuantity();
        this.active = variant.getActive();
        this.defaultVariant = variant.getDefaultVariant();
        this.position = variant.getPosition();
        this.options = new LinkedHashMap<>(variant.getOptions());
        this.createdDate = variant.getCreatedDate();
        this.updatedDate = variant.getUpdatedDate();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
