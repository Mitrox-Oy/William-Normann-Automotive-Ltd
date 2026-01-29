package com.ecommerse.backend.dto.analytics;

/**
 * Request payload for on-demand analytics report generation.
 */
public class GenerateReportRequest {

    private String type;
    private String dateRange;
    private String format;
    private String filters;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }
}
