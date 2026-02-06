package com.ecommerse.backend.controllers;

import com.ecommerse.backend.services.LoadTestDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin tooling endpoints intended for staging/load testing.
 *
 * Guardrails:
 * - Disabled unless admin.tools.enabled=true (via env var ADMIN_TOOLS_ENABLED)
 * - All deletes are restricted to products created by this tool (SKU prefix LT-<runId>-)
 */
@RestController
@RequestMapping("/api/admin/tools/loadtest")
public class AdminToolsController {

    private final LoadTestDataService loadTestDataService;

    @Value("${admin.tools.enabled:false}")
    private boolean adminToolsEnabled;

    public AdminToolsController(LoadTestDataService loadTestDataService) {
        this.loadTestDataService = loadTestDataService;
    }

    public record CreateProductsRequest(Integer count) {
    }

    public record ToolsDisabledResponse(String message) {
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> createProducts(@RequestBody(required = false) CreateProductsRequest request) {
        if (!adminToolsEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ToolsDisabledResponse("Admin tools disabled. Set ADMIN_TOOLS_ENABLED=true on backend."));
        }

        Integer count = request != null ? request.count() : null;
        var result = loadTestDataService.createProducts(count, null);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteProducts(
            @RequestParam String runId,
            @RequestParam(defaultValue = "false") boolean hardDelete) {
        if (!adminToolsEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ToolsDisabledResponse("Admin tools disabled. Set ADMIN_TOOLS_ENABLED=true on backend."));
        }

        var result = loadTestDataService.deleteProducts(runId, hardDelete);
        return ResponseEntity.ok(result);
    }
}
