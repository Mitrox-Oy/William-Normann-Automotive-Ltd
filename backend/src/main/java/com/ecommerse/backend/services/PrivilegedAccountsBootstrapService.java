package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates/updates privileged OWNER/ADMIN accounts from environment variables.
 *
 * Safety:
 * - Disabled by default (must explicitly enable).
 * - Reads passwords only from env/config, never hardcodes them.
 * - Intended as a one-time bootstrap on a fresh deployment; disable afterwards.
 */
@Component
@Order(3) // Run after migrations + non-privileged init
public class PrivilegedAccountsBootstrapService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OwnerWhitelistRepository ownerWhitelistRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${privileged.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${privileged.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${privileged.bootstrap.admin.password:}")
    private String adminPassword;

    // Allows setting secrets via base64 to avoid shell/quoting issues on some platforms.
    @Value("${privileged.bootstrap.admin.password.b64:}")
    private String adminPasswordB64;

    @Value("${privileged.bootstrap.owner1.email:}")
    private String owner1Email;

    @Value("${privileged.bootstrap.owner1.password:}")
    private String owner1Password;

    @Value("${privileged.bootstrap.owner1.password.b64:}")
    private String owner1PasswordB64;

    @Value("${privileged.bootstrap.owner2.email:}")
    private String owner2Email;

    @Value("${privileged.bootstrap.owner2.password:}")
    private String owner2Password;

    @Value("${privileged.bootstrap.owner2.password.b64:}")
    private String owner2PasswordB64;

    public PrivilegedAccountsBootstrapService(
            UserRepository userRepository,
            OwnerWhitelistRepository ownerWhitelistRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.ownerWhitelistRepository = ownerWhitelistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String resolvedAdminPassword = resolvePassword(adminPassword, adminPasswordB64, "admin");
        String resolvedOwner1Password = resolvePassword(owner1Password, owner1PasswordB64, "owner1");
        String resolvedOwner2Password = resolvePassword(owner2Password, owner2PasswordB64, "owner2");

        List<BootstrapUser> users = new ArrayList<>();
        addBootstrapUserIfConfigured(users, "admin", adminEmail, resolvedAdminPassword, User.Role.ADMIN,
                "Privileged admin bootstrap");
        addBootstrapUserIfConfigured(users, "owner1", owner1Email, resolvedOwner1Password, User.Role.OWNER,
                "Privileged owner bootstrap");
        addBootstrapUserIfConfigured(users, "owner2", owner2Email, resolvedOwner2Password, User.Role.OWNER,
                "Privileged owner bootstrap");

        if (users.isEmpty()) {
            throw new IllegalStateException(
                    "Privileged bootstrap is enabled but no privileged accounts are configured.");
        }

        System.out.println("=== PRIVILEGED ACCOUNTS BOOTSTRAP START (enabled=true) ===");

        for (BootstrapUser u : users) {
            upsertWhitelist(u.email, u.notes);
            upsertUser(u.email, u.password, u.role);
        }

        System.out.println("=== PRIVILEGED ACCOUNTS BOOTSTRAP COMPLETE ===");
    }

    private static void addBootstrapUserIfConfigured(
            List<BootstrapUser> users,
            String label,
            String email,
            String password,
            User.Role role,
            String notes
    ) {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPassword = password != null && !password.isBlank();

        if (!hasEmail && !hasPassword) {
            return;
        }

        if (!hasEmail) {
            throw new IllegalStateException(
                    "Privileged bootstrap enabled but required email is missing for " + label + ".");
        }
        if (!hasPassword) {
            throw new IllegalStateException(
                    "Privileged bootstrap enabled but required password is missing for " + label + ".");
        }

        users.add(new BootstrapUser(email, password, role, notes));
    }

    private static String resolvePassword(String plain, String b64, String label) {
        if (b64 != null && !b64.isBlank()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(b64.trim());
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Privileged bootstrap enabled but " + label
                        + " base64 password is invalid.", e);
            }
        }
        return plain;
    }

    private void upsertWhitelist(String email, String notes) {
        Optional<OwnerWhitelist> existing = ownerWhitelistRepository.findByEmail(email);
        OwnerWhitelist entry = existing.orElseGet(() -> new OwnerWhitelist(email, "bootstrap", notes));

        entry.setEmail(email);
        entry.setActive(true);
        entry.setNotes(notes);
        if (entry.getCreatedBy() == null || entry.getCreatedBy().isBlank()) {
            entry.setCreatedBy("bootstrap");
        }

        ownerWhitelistRepository.save(entry);
    }

    private void upsertUser(String email, String rawPassword, User.Role role) {
        Optional<User> existing = userRepository.findByUsername(email);
        User user = existing.orElseGet(User::new);

        user.setUsername(email);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // Avoid nulls in responses/UI.
        if (user.getFirstName() == null) user.setFirstName("");
        if (user.getLastName() == null) user.setLastName("");

        userRepository.save(user);
    }

    private static final class BootstrapUser {
        final String email;
        final String password;
        final User.Role role;
        final String notes;

        BootstrapUser(String email, String password, User.Role role, String notes) {
            this.email = email;
            this.password = password;
            this.role = role;
            this.notes = notes;
        }
    }
}
