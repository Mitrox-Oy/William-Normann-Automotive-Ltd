package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.LeadRequest;
import com.ecommerse.backend.services.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lead")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> submitLead(@Valid @RequestBody LeadRequest request) {
        LeadService.LeadSubmissionResult result = leadService.submitLead(request);

        if (result.spamBlocked()) {
            return ResponseEntity.ok(Map.of("success", true));
        }

        if (!result.sent()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("leadId", result.leadId());
            error.put("error", result.error() != null ? result.error() : "Failed to deliver lead to WhatsApp");
            return ResponseEntity.status(502).body(error);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("leadId", result.leadId());
        response.put("recipient", result.recipient());
        response.put("messageId", result.messageId());
        return ResponseEntity.ok(response);
    }
}