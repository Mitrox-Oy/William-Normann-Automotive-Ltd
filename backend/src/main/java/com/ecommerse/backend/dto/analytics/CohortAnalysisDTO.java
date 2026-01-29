package com.ecommerse.backend.dto.analytics;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Captures cohort retention insight for customer analytics dashboards.
 */
public class CohortAnalysisDTO {

    private String cohortMonth;
    private BigDecimal averageLifetimeValue;
    private Double retentionRate;
    private Integer activeCustomers;
    private Map<Integer, Double> retentionByPeriod;

    public String getCohortMonth() {
        return cohortMonth;
    }

    public void setCohortMonth(String cohortMonth) {
        this.cohortMonth = cohortMonth;
    }

    public BigDecimal getAverageLifetimeValue() {
        return averageLifetimeValue;
    }

    public void setAverageLifetimeValue(BigDecimal averageLifetimeValue) {
        this.averageLifetimeValue = averageLifetimeValue;
    }

    public Double getRetentionRate() {
        return retentionRate;
    }

    public void setRetentionRate(Double retentionRate) {
        this.retentionRate = retentionRate;
    }

    public Integer getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(Integer activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public Map<Integer, Double> getRetentionByPeriod() {
        return retentionByPeriod;
    }

    public void setRetentionByPeriod(Map<Integer, Double> retentionByPeriod) {
        this.retentionByPeriod = retentionByPeriod;
    }
}
