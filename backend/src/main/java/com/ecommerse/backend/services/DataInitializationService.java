package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2) // Run after UserMigrationService
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OwnerWhitelistRepository ownerWhitelistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DATA INITIALIZATION SERVICE START ===");

        try {
            // Create default users if they don't exist
            if (!userRepository.existsByUsername("customer@example.com")) {
                User customer = new User(
                        "customer@example.com",
                        "Test",
                        "Customer",
                        passwordEncoder.encode("password123"),
                        User.Role.CUSTOMER);
                userRepository.save(customer);
                System.out.println("Created default customer: customer@example.com / password123");
            } else {
                System.out.println("Default customer already exists: customer@example.com");
            }

            // Create owner whitelist entries
            createOwnerWhitelistEntry("admin@ecommerce.com", "System", "Main administrator account");
            createOwnerWhitelistEntry("owner@ecommerce.com", "System", "Store owner account");
            createOwnerWhitelistEntry("owner@example.com", "System", "Default owner account");
            createOwnerWhitelistEntry("johannes.hurmerinta@example.com", "System", "Owner account");

            // Create owner accounts with temporary passwords
            createOwnerAccount("admin@ecommerce.com", "TempPassword123!", User.Role.ADMIN);
            createOwnerAccount("owner@ecommerce.com", "TempPassword456!", User.Role.OWNER);
            createOwnerAccount("johannes.hurmerinta@example.com", "TempPassword123!", User.Role.OWNER);

            if (!userRepository.existsByUsername("owner@example.com")) {
                User owner = new User(
                        "owner@example.com",
                        "Store",
                        "Owner",
                        passwordEncoder.encode("password123"),
                        User.Role.OWNER);
                userRepository.save(owner);
                System.out.println("Created default owner: owner@example.com / password123");
            } else {
                System.out.println("Default owner already exists: owner@example.com");
            }

            // EXPLICIT ADMIN ROLE CORRECTION
            fixAdminUserRole();

            System.out.println("=== DATA INITIALIZATION SERVICE COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.out.println("=== DATA INITIALIZATION SERVICE ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void createOwnerWhitelistEntry(String email, String createdBy, String notes) {
        try {
            Optional<OwnerWhitelist> existingEntry = ownerWhitelistRepository.findByEmail(email);

            if (existingEntry.isPresent()) {
                OwnerWhitelist whitelist = existingEntry.get();

                if (!whitelist.isActive()) {
                    whitelist.setActive(true);
                    System.out.println("Re-activated owner whitelist entry for: " + email);
                } else {
                    System.out.println("Owner whitelist entry already exists for: " + email);
                }

                if (notes != null && !notes.isBlank() && (whitelist.getNotes() == null || !whitelist.getNotes().equals(notes))) {
                    whitelist.setNotes(notes);
                }

                ownerWhitelistRepository.save(whitelist);
            } else {
                OwnerWhitelist whitelist = new OwnerWhitelist(email, createdBy, notes);
                ownerWhitelistRepository.save(whitelist);
                System.out.println("Created owner whitelist entry for: " + email);
            }
        } catch (Exception e) {
            System.out.println("Error creating or updating whitelist entry for " + email + ": " + e.getMessage());
        }
    }

    private void createOwnerAccount(String email, String tempPassword, User.Role role) {
        try {
            if (!userRepository.existsByUsername(email)) {
                String firstName = email.equals("admin@ecommerce.com") ? "Admin" : "Store";
                String lastName = email.equals("admin@ecommerce.com") ? "User" : "Owner";
                User owner = new User(email, firstName, lastName, passwordEncoder.encode(tempPassword), role);
                userRepository.save(owner);
                System.out.println("Created account for: " + email + " with role: " + role + " and temporary password: " + tempPassword);
            } else {
                System.out.println("Account already exists for: " + email);

                // CHECK AND FIX EXISTING USER ROLE
                User existingUser = userRepository.findByUsername(email).orElse(null);
                if (existingUser != null) {
                    System.out.println("DEBUG: Existing user " + email + " has role: " + existingUser.getRole());

                    // FIX: Update role if it's wrong
                    if (existingUser.getRole() != role) {
                        System.out.println("WARNING: Role mismatch! Expected: " + role + ", Found: " + existingUser.getRole());
                        User.Role oldRole = existingUser.getRole();
                        existingUser.setRole(role);
                        userRepository.save(existingUser);
                        System.out.println("FIXED: Updated " + email + " role from " + oldRole + " to: " + role);
                    } else {
                        System.out.println("VERIFIED: " + email + " has correct role: " + role);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error creating account for " + email + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Explicitly fix admin user role - ensures admin@ecommerce.com has ADMIN role
     */
    private void fixAdminUserRole() {
        try {
            System.out.println("=== FIXING ADMIN USER ROLE ===");

            User adminUser = userRepository.findByUsername("admin@ecommerce.com").orElse(null);
            if (adminUser != null) {
                System.out.println("Found admin user: " + adminUser.getUsername() + " with current role: " + adminUser.getRole());

                if (adminUser.getRole() != User.Role.ADMIN) {
                    System.out.println("CRITICAL FIX: Admin user has wrong role (" + adminUser.getRole() + "), updating to ADMIN");
                    adminUser.setRole(User.Role.ADMIN);
                    userRepository.save(adminUser);
                    System.out.println("SUCCESS: Admin user role updated to ADMIN");
                } else {
                    System.out.println("VERIFIED: Admin user already has correct ADMIN role");
                }
            } else {
                System.out.println("ERROR: Admin user not found in database!");
            }

            System.out.println("=== ADMIN ROLE FIX COMPLETED ===");
        } catch (Exception e) {
            System.out.println("ERROR fixing admin role: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
