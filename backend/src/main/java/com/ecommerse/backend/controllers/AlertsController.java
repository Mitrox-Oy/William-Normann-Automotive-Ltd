package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.analytics.AlertHistoryDTO;
import com.ecommerse.backend.dto.analytics.CreateAlertRequest;
import com.ecommerse.backend.dto.analytics.UpdateAlertRequest;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.entities.analytics.AnalyticsAlert;
import com.ecommerse.backend.services.analytics.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for alert definitions and acknowledgement workflow.
 */
@RestController
@RequestMapping("/api/alerts")
@PreAuthorize("hasRole('OWNER')")
public class AlertsController {

    private final AlertService alertService;

    public AlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsAlert>> getActiveAlerts() {
        List<AnalyticsAlert> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AlertHistoryDTO>> getAlertHistory(
            @RequestParam(required = false) Boolean acknowledged) {
        List<AlertHistoryDTO> history = alertService.getAlertHistory(acknowledged);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<AnalyticsAlert> createAlert(@RequestBody CreateAlertRequest request) {
        AnalyticsAlert alert = alertService.createAlert(request);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable("id") Long historyId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        alertService.acknowledgeAlert(historyId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalyticsAlert> updateAlert(
            @PathVariable Long id,
            @RequestBody UpdateAlertRequest request) {
        AnalyticsAlert alert = alertService.updateAlert(id, request);
        return ResponseEntity.ok(alert);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}
