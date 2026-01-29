package com.ecommerse.backend.dto.analytics;

import java.math.BigDecimal;

/**
 * Aggregated metrics displayed on analytics dashboards.
 */
public class DashboardMetricsDTO {

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private Double conversionRate = 0.0;
    private BigDecimal averageOrderValue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private Long totalOrders = 0L;
    private TrafficHealthDTO trafficHealth;
    private PeriodComparisonDTO comparison;

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public TrafficHealthDTO getTrafficHealth() {
        return trafficHealth;
    }

    public void setTrafficHealth(TrafficHealthDTO trafficHealth) {
        this.trafficHealth = trafficHealth;
    }

    public PeriodComparisonDTO getComparison() {
        return comparison;
    }

    public void setComparison(PeriodComparisonDTO comparison) {
        this.comparison = comparison;
    }
}
