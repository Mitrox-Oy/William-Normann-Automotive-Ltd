package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2) // Run after UserMigrationService
@Profile({"dev", "docker"})
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DATA INITIALIZATION SERVICE START ===");

        try {
            // Keep initialization minimal and non-privileged.
            // Privileged accounts (OWNER/ADMIN) should be created via explicit scripts/env-driven bootstrap,
            // not hardcoded here.

            // Create default customer user if it doesn't exist
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

            System.out.println("=== DATA INITIALIZATION SERVICE COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.out.println("=== DATA INITIALIZATION SERVICE ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
