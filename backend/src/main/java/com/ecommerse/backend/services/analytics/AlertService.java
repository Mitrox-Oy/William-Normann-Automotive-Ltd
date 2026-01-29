package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.AlertHistoryDTO;
import com.ecommerse.backend.dto.analytics.CreateAlertRequest;
import com.ecommerse.backend.dto.analytics.UpdateAlertRequest;
import com.ecommerse.backend.entities.analytics.AlertHistory;
import com.ecommerse.backend.entities.analytics.AnalyticsAlert;
import com.ecommerse.backend.repositories.analytics.AlertHistoryRepository;
import com.ecommerse.backend.repositories.analytics.AnalyticsAlertRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business logic for configuring analytics alerts and retrieving history.
 */
@Service
public class AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    private final AnalyticsAlertRepository analyticsAlertRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    public AlertService(AnalyticsAlertRepository analyticsAlertRepository,
                        AlertHistoryRepository alertHistoryRepository) {
        this.analyticsAlertRepository = analyticsAlertRepository;
        this.alertHistoryRepository = alertHistoryRepository;
    }

    public List<AnalyticsAlert> getActiveAlerts() {
        return analyticsAlertRepository.findByIsActiveTrue();
    }

    public List<AlertHistoryDTO> getAlertHistory(Boolean acknowledged) {
        List<AlertHistory> historyItems;
        if (acknowledged == null) {
            historyItems = alertHistoryRepository.findAll();
        } else if (Boolean.TRUE.equals(acknowledged)) {
            historyItems = alertHistoryRepository.findByAcknowledgedTrueOrderByTriggeredAtDesc();
        } else {
            historyItems = alertHistoryRepository.findByAcknowledgedFalseOrderByTriggeredAtDesc();
        }

        historyItems.sort((a, b) -> {
            LocalDateTime at = a.getTriggeredAt();
            LocalDateTime bt = b.getTriggeredAt();
            if (at == null || bt == null) {
                return 0;
            }
            return bt.compareTo(at);
        });

        Map<Long, AnalyticsAlert> alertsById = new HashMap<>();
        List<AlertHistoryDTO> dtos = new ArrayList<>();
        for (AlertHistory history : historyItems) {
            AnalyticsAlert alert = alertsById.computeIfAbsent(history.getAlertId(), id ->
                    analyticsAlertRepository.findById(id).orElse(null));

            AlertHistoryDTO dto = new AlertHistoryDTO();
            dto.setId(history.getId());
            dto.setAlertId(history.getAlertId());
            dto.setAlertName(alert != null ? alert.getAlertName() : "Alert " + history.getAlertId());
            dto.setMetricValue(history.getMetricValue());
            dto.setMessage(history.getMessage());
            dto.setPriority(alert != null ? alert.getPriority() : 3);
            dto.setAcknowledged(history.getAcknowledged());
            dto.setTriggeredAt(history.getTriggeredAt());
            dto.setAcknowledgedAt(history.getAcknowledgedAt());
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public AnalyticsAlert createAlert(CreateAlertRequest request) {
        AnalyticsAlert alert = new AnalyticsAlert();
        alert.setAlertName(request.getAlertName());
        alert.setMetricName(request.getMetricName());
        alert.setCondition(request.getCondition());
        alert.setThresholdJson(request.getThresholdJson());
        alert.setNotificationChannel(request.getNotificationChannel());
        alert.setRecipients(request.getRecipients());
        alert.setPriority(request.getPriority() != null ? request.getPriority() : 3);
        LOGGER.info("Creating analytics alert {}", request.getAlertName());
        return analyticsAlertRepository.save(alert);
    }

    @Transactional
    public void acknowledgeAlert(Long alertHistoryId, Long acknowledgedBy) {
        AlertHistory history = alertHistoryRepository.findById(alertHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("Alert history entry not found: " + alertHistoryId));
        history.setAcknowledged(Boolean.TRUE);
        history.setAcknowledgedBy(acknowledgedBy);
        history.setAcknowledgedAt(LocalDateTime.now());
        alertHistoryRepository.save(history);
        LOGGER.info("Alert history {} acknowledged by {}", alertHistoryId, acknowledgedBy);
    }

    @Transactional
    public AnalyticsAlert updateAlert(Long id, UpdateAlertRequest request) {
        AnalyticsAlert alert = analyticsAlertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analytics alert not found: " + id));

        Optional.ofNullable(request.getAlertName()).ifPresent(alert::setAlertName);
        Optional.ofNullable(request.getMetricName()).ifPresent(alert::setMetricName);
        Optional.ofNullable(request.getCondition()).ifPresent(alert::setCondition);
        Optional.ofNullable(request.getThresholdJson()).ifPresent(alert::setThresholdJson);
        Optional.ofNullable(request.getNotificationChannel()).ifPresent(alert::setNotificationChannel);
        Optional.ofNullable(request.getRecipients()).ifPresent(alert::setRecipients);
        Optional.ofNullable(request.getPriority()).ifPresent(alert::setPriority);
        if (request.getIsActive() != null) {
            alert.setIsActive(request.getIsActive());
        }
        return analyticsAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long id) {
        analyticsAlertRepository.deleteById(id);
    }

    /**
     * Create an AlertHistory record for a simple system event such as order paid.
     * If there is no configured AnalyticsAlert, stores with alertId = 0.
     */
    @Transactional
    public void recordSystemAlert(String message, String metricValue) {
        AlertHistory history = new AlertHistory();
        history.setAlertId(0L);
        history.setTriggeredAt(LocalDateTime.now());
        history.setMessage(message);
        history.setMetricValue(metricValue);
        alertHistoryRepository.save(history);
    }
}
