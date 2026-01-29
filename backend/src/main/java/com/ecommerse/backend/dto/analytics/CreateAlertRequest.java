package com.ecommerse.backend.dto.analytics;

/**
 * Request payload for creating analytics alerts.
 */
public class CreateAlertRequest {

    private String alertName;
    private String metricName;
    private String condition;
    private String thresholdJson;
    private String notificationChannel;
    private String recipients;
    private Integer priority;

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getThresholdJson() {
        return thresholdJson;
    }

    public void setThresholdJson(String thresholdJson) {
        this.thresholdJson = thresholdJson;
    }

    public String getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
