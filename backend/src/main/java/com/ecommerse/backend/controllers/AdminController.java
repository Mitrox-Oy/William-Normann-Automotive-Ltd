package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.services.WhitelistManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing whitelist operations
 * Only accessible by users with ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private WhitelistManagementService whitelistService;

    /**
     * Get all whitelist entries for admin dashboard
     * @return List of active whitelist entries
     */
    @GetMapping("/whitelist")
    public ResponseEntity<List<OwnerWhitelist>> getWhitelistEntries() {
        try {
            List<OwnerWhitelist> entries = whitelistService.getAllActiveEntries();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            System.err.println("Error fetching whitelist entries: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get whitelist statistics
     * @return Statistics about the whitelist
     */
    @GetMapping("/whitelist/stats")
    public ResponseEntity<WhitelistManagementService.WhitelistStats> getWhitelistStats() {
        try {
            WhitelistManagementService.WhitelistStats stats = whitelistService.getWhitelistStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching whitelist stats: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add email to whitelist
     * @param request Request containing email, optional notes, and optional temporary password
     * @param currentUser Currently authenticated admin user
     * @return Created whitelist entry
     */
    @PostMapping("/whitelist")
    public ResponseEntity<?> addToWhitelist(
            @Valid @RequestBody AddToWhitelistRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            String adminUsername = currentUser != null ? currentUser.getUsername() : "system";
            WhitelistManagementService.WhitelistAddResult result = whitelistService.addToWhitelist(
                request.getEmail(),
                adminUsername,
                request.getNotes(),
                request.getTemporaryPassword()
            );

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("entry", result.entry());
            response.put("newUserCreated", result.newUserCreated());
            if (result.temporaryPassword() != null) {
                response.put("temporaryPassword", result.temporaryPassword());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error adding to whitelist: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to add email to whitelist"));
        }
    }
    /**
     * Remove email from whitelist
     * @param id ID of the whitelist entry to remove
     * @param currentUser Currently authenticated admin user
     * @return Success response
     */
    @DeleteMapping("/whitelist/{id}")
    public ResponseEntity<?> removeFromWhitelist(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            String adminUsername = currentUser != null ? currentUser.getUsername() : "system";
            whitelistService.removeFromWhitelist(id, adminUsername);
            return ResponseEntity.ok(Map.of("message", "Email removed from whitelist successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error removing from whitelist: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to remove email from whitelist"));
        }
    }

    /**
     * DTO for adding email to whitelist
     */
    public static class AddToWhitelistRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String notes;

        @Size(min = 12, message = "Temporary password must be at least 12 characters")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9!@#$%]+$",
            message = "Temporary password must include letters, numbers, and can only use !@#$% as special characters"
        )
        private String temporaryPassword;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getTemporaryPassword() {
            return temporaryPassword;
        }

        public void setTemporaryPassword(String temporaryPassword) {
            if (temporaryPassword == null) {
                this.temporaryPassword = null;
                return;
            }

            String trimmed = temporaryPassword.trim();
            this.temporaryPassword = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
