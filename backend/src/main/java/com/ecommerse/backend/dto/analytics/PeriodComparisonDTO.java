package com.ecommerse.backend.dto.analytics;

/**
 * Tracks percentage deltas versus a previous period for key KPIs.
 */
public class PeriodComparisonDTO {

    private Double revenueGrowth = 0.0;
    private Double conversionGrowth = 0.0;
    private Double aovGrowth = 0.0;
    private Double sessionGrowth = 0.0;

    public Double getRevenueGrowth() {
        return revenueGrowth;
    }

    public void setRevenueGrowth(Double revenueGrowth) {
        this.revenueGrowth = revenueGrowth;
    }

    public Double getConversionGrowth() {
        return conversionGrowth;
    }

    public void setConversionGrowth(Double conversionGrowth) {
        this.conversionGrowth = conversionGrowth;
    }

    public Double getAovGrowth() {
        return aovGrowth;
    }

    public void setAovGrowth(Double aovGrowth) {
        this.aovGrowth = aovGrowth;
    }

    public Double getSessionGrowth() {
        return sessionGrowth;
    }

    public void setSessionGrowth(Double sessionGrowth) {
        this.sessionGrowth = sessionGrowth;
    }
}
