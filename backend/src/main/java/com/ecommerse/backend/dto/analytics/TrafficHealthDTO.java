package com.ecommerse.backend.dto.analytics;

/**
 * Captures traffic and engagement signals for dashboard health widgets.
 */
public class TrafficHealthDTO {

    private Long totalSessions = 0L;
    private Long productViews = 0L;
    private Long cartActions = 0L;
    private Long convertedSessions = 0L;
    private Double bounceRate = 0.0;

    public Long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Long getProductViews() {
        return productViews;
    }

    public void setProductViews(Long productViews) {
        this.productViews = productViews;
    }

    public Long getCartActions() {
        return cartActions;
    }

    public void setCartActions(Long cartActions) {
        this.cartActions = cartActions;
    }

    public Long getConvertedSessions() {
        return convertedSessions;
    }

    public void setConvertedSessions(Long convertedSessions) {
        this.convertedSessions = convertedSessions;
    }

    public Double getBounceRate() {
        return bounceRate;
    }

    public void setBounceRate(Double bounceRate) {
        this.bounceRate = bounceRate;
    }
}
