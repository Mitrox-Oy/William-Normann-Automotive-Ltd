package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for managing owner whitelist operations
 * Handles CRUD operations for whitelist entries with proper validation
 */
@Service
@Transactional
public class WhitelistManagementService {

    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final String PASSWORD_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final Pattern TEMP_PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9!@#$%]+$");
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private OwnerWhitelistRepository whitelistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get all active whitelist entries ordered by creation date
     * @return List of active whitelist entries
     */
    public List<OwnerWhitelist> getAllActiveEntries() {
        return whitelistRepository.findAll().stream()
            .filter(OwnerWhitelist::isActive)
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .toList();
    }

    /**
     * Add email to whitelist with validation
     * @param email Email to add to whitelist
     * @param adminUsername Username of admin performing the action
     * @param notes Optional notes about the whitelist entry
     * @param adminProvidedPassword Optional administrator-specified temporary password
     * @return Result containing created whitelist entry and temporary password details
     * @throws IllegalArgumentException if email is invalid or already exists
     */
    public WhitelistAddResult addToWhitelist(String email, String adminUsername, String notes, String adminProvidedPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format - must contain @ symbol");
        }

        String normalizedEmail = email.toLowerCase().trim();

        if (whitelistRepository.existsByEmailAndIsActive(normalizedEmail, true)) {
            throw new IllegalArgumentException("Email is already whitelisted");
        }

        String normalizedAdminPassword = normalizeTemporaryPassword(adminProvidedPassword);
        if (normalizedAdminPassword != null) {
            validateTemporaryPassword(normalizedAdminPassword);
        }

        OwnerWhitelist whitelist = new OwnerWhitelist(
            normalizedEmail,
            adminUsername,
            notes != null ? notes.trim() : null
        );

        OwnerWhitelist saved = whitelistRepository.save(whitelist);
        System.out.println("Admin " + adminUsername + " added " + normalizedEmail + " to whitelist");

        User existingUser = userRepository.findByUsername(normalizedEmail).orElse(null);
        boolean newUserCreated = false;
        String temporaryPassword = normalizedAdminPassword != null
            ? normalizedAdminPassword
            : generateTemporaryPassword();

        if (existingUser == null) {
            User newOwner = new User(
                    normalizedEmail,
                    "Store",
                    "Owner",
                    passwordEncoder.encode(temporaryPassword),
                    User.Role.OWNER
            );
            userRepository.save(newOwner);
            newUserCreated = true;
            System.out.println("Created owner account for " + normalizedEmail + (normalizedAdminPassword != null ? " with admin-provided temporary password" : " with generated temporary password"));
        } else {
            existingUser.setPassword(passwordEncoder.encode(temporaryPassword));
            if (existingUser.getRole() != User.Role.OWNER && existingUser.getRole() != User.Role.ADMIN) {
                existingUser.setRole(User.Role.OWNER);
            }
            if (existingUser.getFirstName() == null || existingUser.getFirstName().isBlank()) {
                existingUser.setFirstName("Store");
            }
            if (existingUser.getLastName() == null || existingUser.getLastName().isBlank()) {
                existingUser.setLastName("Owner");
            }
            userRepository.save(existingUser);
            System.out.println("Refreshed owner credentials for " + normalizedEmail + (normalizedAdminPassword != null ? " using admin-provided temporary password" : " and generated new temporary password"));
        }

        return new WhitelistAddResult(saved, temporaryPassword, newUserCreated);
    }

    /**
     * Remove email from whitelist (soft delete)
     * @param whitelistId ID of the whitelist entry to remove
     * @param adminUsername Username of admin performing the action
     * @throws IllegalArgumentException if whitelist entry not found or already inactive
     */
    public void removeFromWhitelist(Long whitelistId, String adminUsername) {
        OwnerWhitelist whitelist = whitelistRepository.findById(whitelistId)
            .orElseThrow(() -> new IllegalArgumentException("Whitelist entry not found"));

        if (!whitelist.isActive()) {
            throw new IllegalArgumentException("Whitelist entry is already inactive");
        }

        whitelist.setActive(false);
        whitelist.setUpdatedAt(LocalDateTime.now());

        whitelistRepository.save(whitelist);

        System.out.println("Admin " + adminUsername + " removed " + whitelist.getEmail() + " from whitelist");
    }

    /**
     * Get whitelist statistics
     * @return Statistics about the whitelist
     */
    public WhitelistStats getWhitelistStats() {
        List<OwnerWhitelist> allEntries = whitelistRepository.findAll();

        long totalActive = allEntries.stream()
            .filter(OwnerWhitelist::isActive)
            .count();

        long totalInactive = allEntries.stream()
            .filter(entry -> !entry.isActive())
            .count();

        List<OwnerWhitelist> recentEntries = allEntries.stream()
            .filter(OwnerWhitelist::isActive)
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(5)
            .toList();

        return new WhitelistStats(totalActive, totalInactive, recentEntries);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARSET.length());
            password.append(PASSWORD_CHARSET.charAt(index));
        }
        return password.toString();
    }

    private String normalizeTemporaryPassword(String password) {
        if (password == null) {
            return null;
        }

        String trimmed = password.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateTemporaryPassword(String password) {
        if (password.length() < TEMP_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Temporary password must be at least " + TEMP_PASSWORD_LENGTH + " characters long");
        }

        if (!TEMP_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Temporary password must contain letters, numbers, and may only include !@#$% as special characters");
        }
    }

    /**
     * Simple DTO for whitelist statistics
     */
    public static class WhitelistStats {
        private final long totalActive;
        private final long totalInactive;
        private final List<OwnerWhitelist> recentEntries;

        public WhitelistStats(long totalActive, long totalInactive, List<OwnerWhitelist> recentEntries) {
            this.totalActive = totalActive;
            this.totalInactive = totalInactive;
            this.recentEntries = recentEntries;
        }

        public long getTotalActive() { return totalActive; }
        public long getTotalInactive() { return totalInactive; }
        public List<OwnerWhitelist> getRecentEntries() { return recentEntries; }
    }

    public record WhitelistAddResult(OwnerWhitelist entry, String temporaryPassword, boolean newUserCreated) { }
}


