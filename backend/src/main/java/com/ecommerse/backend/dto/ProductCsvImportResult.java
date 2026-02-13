package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "CSV product import summary and row outcomes")
public class ProductCsvImportResult {

    @Schema(description = "Whether this run was dry-run only", example = "true")
    private boolean dryRun;

    @Schema(description = "Total non-empty rows processed", example = "150")
    private int totalRows;

    @Schema(description = "Successfully imported/validated rows", example = "142")
    private int successCount;

    @Schema(description = "Failed rows", example = "8")
    private int failedCount;

    @Schema(description = "Per-row details")
    private List<ProductCsvImportRowResult> rows = new ArrayList<>();

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<ProductCsvImportRowResult> getRows() {
        return rows;
    }

    public void setRows(List<ProductCsvImportRowResult> rows) {
        this.rows = rows;
    }
}
