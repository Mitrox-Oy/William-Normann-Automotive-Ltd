package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.AuthResponse;
import com.ecommerse.backend.dto.ChangePasswordRequest;
import com.ecommerse.backend.dto.LoginRequest;
import com.ecommerse.backend.dto.OwnerLoginRequest;
import com.ecommerse.backend.dto.RegisterRequest;
import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OwnerWhitelistRepository ownerWhitelistRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        // Only allow CUSTOMER role through regular login
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found with username: " + loginRequest.getUsername()));

        if (user.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("Access denied. Please use the appropriate login portal.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User userDetails = (User) authentication.getPrincipal();

        return new AuthResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getRole(),
                userDetails.getFirstName(),
                userDetails.getLastName());
    }

    public AuthResponse authenticateOwner(OwnerLoginRequest ownerLoginRequest) {
        String email = ownerLoginRequest.getEmail();
        System.out.println("=== OWNER LOGIN DEBUG ===");
        System.out.println("Attempting owner login for email: " + email);

        try {
            // Check if email is in whitelist
            System.out.println("Step 1: Checking whitelist for email: " + email);
            Optional<OwnerWhitelist> whitelistOpt = ownerWhitelistRepository.findByEmailAndIsActive(email, true);
            if (whitelistOpt.isEmpty()) {
                System.out.println("FAILURE: Email not found in whitelist or not active");
                // Let's check if email exists at all
                Optional<OwnerWhitelist> anyEntry = ownerWhitelistRepository.findByEmail(email);
                if (anyEntry.isPresent()) {
                    System.out.println("Email exists but isActive = " + anyEntry.get().isActive());
                    throw new RuntimeException("Owner account is deactivated. Please contact system administrator.");
                } else {
                    System.out.println("Email does not exist in whitelist at all");
                    throw new RuntimeException("Access denied. Email not authorized for owner access.");
                }
            }
            System.out.println("Step 1: PASSED - Email found in whitelist");

            // Find user with OWNER role
            System.out.println("Step 2: Looking for user account with email: " + email);
            Optional<User> userOpt = userRepository.findByUsername(email);
            if (userOpt.isEmpty()) {
                System.out.println("FAILURE: User account not found");
                throw new RuntimeException("Owner account not found. Please contact system administrator.");
            }
            System.out.println("Step 2: PASSED - User account found");

            User user = userOpt.get();
            System.out.println("User details: ID=" + user.getId() + ", Role=" + user.getRole() + ", FirstName="
                    + user.getFirstName() + ", LastName=" + user.getLastName());

            if (user.getRole() != User.Role.OWNER && user.getRole() != User.Role.ADMIN) {
                System.out.println("FAILURE: User role is " + user.getRole() + " but expected OWNER or ADMIN");
                throw new RuntimeException("Access denied. Account does not have owner or admin privileges.");
            }
            System.out.println("Step 3: PASSED - User has OWNER or ADMIN role");

            // Authenticate password
            System.out.println("Step 4: Authenticating password...");
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, ownerLoginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);

                System.out.println("Step 4: PASSED - Authentication successful");
                System.out.println("JWT generated successfully");

                User userDetails = (User) authentication.getPrincipal();

                return new AuthResponse(jwt,
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getRole(),
                        userDetails.getFirstName(),
                        userDetails.getLastName());
            } catch (Exception authEx) {
                System.out.println("FAILURE: Authentication failed - " + authEx.getMessage());
                System.out.println("Auth exception class: " + authEx.getClass().getSimpleName());
                throw new RuntimeException("Invalid password or authentication failed: " + authEx.getMessage());
            }
        } catch (Exception e) {
            System.out.println("=== OWNER LOGIN ERROR ===");
            System.out.println("Error: " + e.getMessage());
            System.out.println("Exception class: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw e;
        }
    }

    public AuthResponse registerUser(RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Create new user's account (always as CUSTOMER)
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                encoder.encode(signUpRequest.getPassword()),
                User.Role.CUSTOMER);

        userRepository.save(user);

        // Generate JWT token for the new user
        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());

        return new AuthResponse(jwt, user.getId(), user.getUsername(), user.getRole(),
                user.getFirstName(), user.getLastName());
    }

    public void changePassword(String username, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!encoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Verify new password and confirm password match
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        // Update password
        user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }

    public void deleteUserAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only allow customers to delete their own accounts
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("Only customer accounts can be deleted through this endpoint.");
        }

        userRepository.delete(user);
    }

    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUserProfile(String username, String firstName, String lastName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);

        return userRepository.save(user);
    }
}
