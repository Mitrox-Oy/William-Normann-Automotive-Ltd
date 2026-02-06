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

    @Value("${privileged.bootstrap.owner1.email:}")
    private String owner1Email;

    @Value("${privileged.bootstrap.owner1.password:}")
    private String owner1Password;

    @Value("${privileged.bootstrap.owner2.email:}")
    private String owner2Email;

    @Value("${privileged.bootstrap.owner2.password:}")
    private String owner2Password;

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

        List<BootstrapUser> users = new ArrayList<>();
        users.add(new BootstrapUser(adminEmail, adminPassword, User.Role.ADMIN, "Privileged admin bootstrap"));
        users.add(new BootstrapUser(owner1Email, owner1Password, User.Role.OWNER, "Privileged owner bootstrap"));
        users.add(new BootstrapUser(owner2Email, owner2Password, User.Role.OWNER, "Privileged owner bootstrap"));

        // Fail-fast if enabled but incomplete.
        for (BootstrapUser u : users) {
            if (u.email == null || u.email.isBlank()) {
                throw new IllegalStateException("Privileged bootstrap enabled but required email is missing.");
            }
            if (u.password == null || u.password.isBlank()) {
                throw new IllegalStateException("Privileged bootstrap enabled but required password is missing.");
            }
        }

        System.out.println("=== PRIVILEGED ACCOUNTS BOOTSTRAP START (enabled=true) ===");

        for (BootstrapUser u : users) {
            upsertWhitelist(u.email, u.notes);
            upsertUser(u.email, u.password, u.role);
        }

        System.out.println("=== PRIVILEGED ACCOUNTS BOOTSTRAP COMPLETE ===");
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

