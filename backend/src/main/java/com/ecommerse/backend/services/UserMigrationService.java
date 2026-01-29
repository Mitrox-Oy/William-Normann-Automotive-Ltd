package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Order(1) // Run before DataInitializationService
public class UserMigrationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        ensureRoleConstraintSupportsAdmin();
        migrateExistingUsers();
        ensureAdminRole();
    }

    private void ensureRoleConstraintSupportsAdmin() {
        try {
            entityManager.createNativeQuery("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER','OWNER','ADMIN'))").executeUpdate();
            System.out.println("Ensured users_role_check allows ADMIN role");
        } catch (Exception ex) {
            System.out.println("WARNING: Failed to update users_role_check constraint: " + ex.getMessage());
        }
    }

    private void migrateExistingUsers() {
        List<User> usersWithoutNames = userRepository.findAll().stream()
                .filter(user -> user.getFirstName() == null || user.getLastName() == null)
                .toList();

        for (User user : usersWithoutNames) {
            if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                // Set default first name based on role
                if (user.getRole() == User.Role.OWNER) {
                    user.setFirstName("Store");
                } else {
                    user.setFirstName("Customer");
                }
            }

            if (user.getLastName() == null || user.getLastName().isEmpty()) {
                // Set default last name based on role
                if (user.getRole() == User.Role.OWNER) {
                    user.setLastName("Owner");
                } else {
                    user.setLastName("User");
                }
            }

            userRepository.save(user);
            System.out.println("Updated user: " + user.getUsername() + " with names: " +
                    user.getFirstName() + " " + user.getLastName());
        }
    }

    private void ensureAdminRole() {
        userRepository.findByUsername("admin@ecommerce.com").ifPresent(admin -> {
            if (admin.getRole() != User.Role.ADMIN) {
                User.Role previousRole = admin.getRole();
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
                System.out.println("Updated admin@ecommerce.com role from " + previousRole + " to ADMIN");
            } else {
                System.out.println("admin@ecommerce.com already has ADMIN role");
            }
        });
    }
}
