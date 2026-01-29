package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DEBUG CONTROLLER - REMOVE IN PRODUCTION
 * This controller helps debug authentication issues
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OwnerWhitelistRepository ownerWhitelistRepository;

    /**
     * Check if a specific email exists in whitelist and user tables
     */
    @GetMapping("/check-owner/{email}")
    public ResponseEntity<Map<String, Object>> checkOwnerStatus(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();

        // Check whitelist
        Optional<OwnerWhitelist> whitelist = ownerWhitelistRepository.findByEmail(email);
        result.put("emailExists", whitelist.isPresent());
        if (whitelist.isPresent()) {
            result.put("whitelistActive", whitelist.get().isActive());
            result.put("whitelistCreatedBy", whitelist.get().getCreatedBy());
            result.put("whitelistCreatedAt", whitelist.get().getCreatedAt());
        }

        // Check user account
        Optional<User> user = userRepository.findByUsername(email);
        result.put("userExists", user.isPresent());
        if (user.isPresent()) {
            result.put("userRole", user.get().getRole());
            result.put("userId", user.get().getId());
            result.put("userFirstName", user.get().getFirstName());
            result.put("userLastName", user.get().getLastName());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * List all whitelist entries
     */
    @GetMapping("/whitelist-all")
    public ResponseEntity<List<OwnerWhitelist>> getAllWhitelist() {
        List<OwnerWhitelist> whitelist = ownerWhitelistRepository.findAll();
        return ResponseEntity.ok(whitelist);
    }

    /**
     * List all owner users
     */
    @GetMapping("/owners-all")
    public ResponseEntity<List<User>> getAllOwners() {
        List<User> owners = userRepository.findByRole(User.Role.OWNER);
        return ResponseEntity.ok(owners);
    }

    /**
     * Test whitelist query specifically
     */
    @GetMapping("/test-whitelist/{email}")
    public ResponseEntity<Map<String, Object>> testWhitelistQuery(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();

        // Test the exact query used in authentication
        Optional<OwnerWhitelist> activeEntry = ownerWhitelistRepository.findByEmailAndIsActive(email, true);
        result.put("activeEntryFound", activeEntry.isPresent());

        Optional<OwnerWhitelist> inactiveEntry = ownerWhitelistRepository.findByEmailAndIsActive(email, false);
        result.put("inactiveEntryFound", inactiveEntry.isPresent());

        boolean existsAndActive = ownerWhitelistRepository.existsByEmailAndIsActive(email, true);
        result.put("existsAndActive", existsAndActive);

        return ResponseEntity.ok(result);
    }

    /**
     * Test JSON parsing without authentication
     */
    @PostMapping(value = "/test-json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> testJson(@RequestBody String rawBody) {
        System.out.println("=== RAW BODY TEST ===");
        System.out.println("Raw body: " + rawBody);
        return ResponseEntity.ok("Raw body received: " + rawBody);
    }

    /**
     * Test OwnerLoginRequest DTO parsing without authentication
     */
    @PostMapping(value = "/test-owner-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> testOwnerRequest(@RequestBody com.ecommerse.backend.dto.OwnerLoginRequest request) {
        System.out.println("=== OWNER REQUEST TEST ===");
        System.out.println("Request object: " + request);
        if (request != null) {
            System.out.println("Email: " + request.getEmail());
            System.out.println(
                    "Password length: " + (request.getPassword() != null ? request.getPassword().length() : "null"));
        }
        return ResponseEntity.ok("Owner request received - Email: " + (request != null ? request.getEmail() : "null"));
    }
}
