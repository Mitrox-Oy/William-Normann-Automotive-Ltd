package com.ecommerse.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated metrics owners can use to monitor Stripe paid orders.
 */
public class StripePaidOrdersDashboardDTO {

    private long totalPaidOrders;
    private long pendingPaymentIntents;
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;
    private BigDecimal averageOrderValue = BigDecimal.ZERO;
    private LocalDateTime lastPaidAt;
    private List<StripePaidOrderSummaryDTO> recentPaidOrders = new ArrayList<>();

    public long getTotalPaidOrders() {
        return totalPaidOrders;
    }

    public void setTotalPaidOrders(long totalPaidOrders) {
        this.totalPaidOrders = totalPaidOrders;
    }

    public long getPendingPaymentIntents() {
        return pendingPaymentIntents;
    }

    public void setPendingPaymentIntents(long pendingPaymentIntents) {
        this.pendingPaymentIntents = pendingPaymentIntents;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public LocalDateTime getLastPaidAt() {
        return lastPaidAt;
    }

    public void setLastPaidAt(LocalDateTime lastPaidAt) {
        this.lastPaidAt = lastPaidAt;
    }

    public List<StripePaidOrderSummaryDTO> getRecentPaidOrders() {
        return recentPaidOrders;
    }

    public void setRecentPaidOrders(List<StripePaidOrderSummaryDTO> recentPaidOrders) {
        this.recentPaidOrders = recentPaidOrders != null ? new ArrayList<>(recentPaidOrders) : new ArrayList<>();
    }
}
