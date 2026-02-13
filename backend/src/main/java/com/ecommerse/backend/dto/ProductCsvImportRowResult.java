package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Per-row outcome for CSV product import")
public class ProductCsvImportRowResult {

    @Schema(description = "CSV row number (1-based, including header)", example = "2")
    private int rowNumber;

    @Schema(description = "Result status", example = "imported")
    private String status;

    @Schema(description = "Product name", example = "BMW 330i M Sport")
    private String name;

    @Schema(description = "Resolved SKU", example = "CAR-BMW-330I-001")
    private String sku;

    @Schema(description = "Created product ID (import mode)", example = "124")
    private Long productId;

    @Schema(description = "Row validation/import errors")
    private List<String> errors = new ArrayList<>();

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
