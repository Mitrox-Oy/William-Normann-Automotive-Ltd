package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.analytics.AnalyticsEvent;
import com.ecommerse.backend.services.analytics.AnalyticsEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@org.springframework.web.bind.annotation.CrossOrigin(
        originPatterns = "${cors.allowed-origins:*}",
        allowCredentials = "true",
        allowedHeaders = "*",
        methods = { RequestMethod.POST, RequestMethod.OPTIONS }
)
public class PublicAnalyticsController {

    private final AnalyticsEventService service;

    public PublicAnalyticsController(AnalyticsEventService service) {
        this.service = service;
    }

    @PostMapping("/public/analytics/events")
    public ResponseEntity<Void> ingestEvent(@RequestBody AnalyticsEvent event) {
        service.ingest(event);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/public/analytics/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @GetMapping("/analytics/summary")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(service.getSummary(days));
    }
}



