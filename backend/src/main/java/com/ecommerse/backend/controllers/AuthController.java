package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.AuthResponse;
import com.ecommerse.backend.dto.ChangePasswordRequest;
import com.ecommerse.backend.dto.ErrorResponse;
import com.ecommerse.backend.dto.LoginRequest;
import com.ecommerse.backend.dto.OwnerLoginRequest;
import com.ecommerse.backend.dto.RegisterRequest;
import com.ecommerse.backend.dto.UpdateProfileRequest;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Invalid username or password",
                    HttpStatus.UNAUTHORIZED.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping(value = "/owner/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticateOwner(@RequestBody OwnerLoginRequest ownerLoginRequest) {
        System.out.println("=== CONTROLLER DEBUG ===");
        System.out.println("Received owner login request");
        System.out.println("Request object: " + ownerLoginRequest);

        // Manual validation instead of @Valid to avoid JSON parsing conflicts
        if (ownerLoginRequest == null) {
            System.out.println("ERROR: Request body is null");
            ErrorResponse errorResponse = new ErrorResponse(
                    "Request body is required",
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (ownerLoginRequest.getEmail() == null || ownerLoginRequest.getEmail().trim().isEmpty()) {
            System.out.println("ERROR: Email is null or empty");
            ErrorResponse errorResponse = new ErrorResponse(
                    "Email is required",
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (ownerLoginRequest.getPassword() == null || ownerLoginRequest.getPassword().trim().isEmpty()) {
            System.out.println("ERROR: Password is null or empty");
            ErrorResponse errorResponse = new ErrorResponse(
                    "Password is required",
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        System.out.println("Email: " + ownerLoginRequest.getEmail());
        System.out.println("Password length: " + ownerLoginRequest.getPassword().length());

        try {
            AuthResponse authResponse = authService.authenticateOwner(ownerLoginRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            System.out.println("=== CONTROLLER ERROR ===");
            System.out.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    HttpStatus.UNAUTHORIZED.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        try {
            AuthResponse authResponse = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Registration failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            authService.changePassword(userDetails.getUsername(), changePasswordRequest);
            return ResponseEntity.ok().body("{\"message\":\"Password changed successfully\"}");
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount() {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            authService.deleteUserAccount(userDetails.getUsername());
            return ResponseEntity.ok().body("{\"message\":\"Account deleted successfully\"}");
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        System.out.println("=== GET USER PROFILE REQUEST START ===");

        try {
            // Step 1: Verify Security Context
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication object: " + authentication);
            System.out.println("Authentication class: "
                    + (authentication != null ? authentication.getClass().getSimpleName() : "null"));
            System.out.println(
                    "Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));

            if (authentication == null) {
                throw new RuntimeException("No authentication in security context");
            }

            if (!authentication.isAuthenticated()) {
                throw new RuntimeException("User is not authenticated");
            }

            // Step 2: Extract User Details
            Object principal = authentication.getPrincipal();
            System.out.println("Principal object: " + principal);
            System.out
                    .println("Principal class: " + (principal != null ? principal.getClass().getSimpleName() : "null"));

            if (!(principal instanceof UserDetails)) {
                throw new RuntimeException("Principal is not UserDetails, got: "
                        + (principal != null ? principal.getClass().getSimpleName() : "null"));
            }

            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();
            System.out.println("Username from security context: " + username);

            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("Username is null or empty");
            }

            // Step 3: Database Query with Enhanced Error Handling
            System.out.println("Attempting to fetch user from database...");
            User user;
            try {
                user = authService.getUserProfile(username);
                System.out.println("Database query successful");
            } catch (Exception dbError) {
                System.out.println("Database query failed: " + dbError.getMessage());
                dbError.printStackTrace();

                // Try alternative query methods for debugging
                try {
                    boolean userExists = userRepository.existsByUsername(username);
                    System.out.println("User exists check: " + userExists);
                } catch (Exception existsError) {
                    System.out.println("Exists check also failed: " + existsError.getMessage());
                }

                throw new RuntimeException("Database error: " + dbError.getMessage(), dbError);
            }

            // Step 4: Validate User Data
            if (user == null) {
                throw new RuntimeException("User object is null after database query");
            }

            System.out.println("User found - ID: " + user.getId() + ", Username: " + user.getUsername() +
                    ", Role: " + user.getRole() + ", FirstName: " + user.getFirstName() +
                    ", LastName: " + user.getLastName());

            // Step 5: Build Response with Null Safety
            var userProfile = new java.util.HashMap<String, Object>();
            userProfile.put("id", user.getId());
            userProfile.put("username", user.getUsername());
            userProfile.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            userProfile.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            userProfile.put("role", user.getRole() != null ? user.getRole() : "UNKNOWN");
            userProfile.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");

            System.out.println("Successfully built user profile response");
            System.out.println("=== GET USER PROFILE REQUEST SUCCESS ===");
            return ResponseEntity.ok(userProfile);

        } catch (ClassCastException e) {
            System.out.println("=== SECURITY CONTEXT CLASS CAST ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

            ErrorResponse errorResponse = new ErrorResponse(
                    "Authentication context error: Invalid principal type",
                    HttpStatus.UNAUTHORIZED.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (RuntimeException e) {
            System.out.println("=== GET USER PROFILE RUNTIME ERROR ===");
            System.out.println("Error: " + e.getMessage());
            System.out.println("Exception class: " + e.getClass().getSimpleName());
            e.printStackTrace();

            // Determine appropriate HTTP status based on error type
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (e.getMessage().contains("not found") || e.getMessage().contains("not authenticated")) {
                status = HttpStatus.UNAUTHORIZED;
            } else if (e.getMessage().contains("Database")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            }

            ErrorResponse errorResponse = new ErrorResponse(
                    "Profile error: " + e.getMessage(),
                    status.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            System.out.println("=== GET USER PROFILE UNEXPECTED ERROR ===");
            System.out.println("Error: " + e.getMessage());
            System.out.println("Exception class: " + e.getClass().getSimpleName());
            e.printStackTrace();

            ErrorResponse errorResponse = new ErrorResponse(
                    "Unexpected error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            var user = authService.updateUserProfile(
                    userDetails.getUsername(),
                    updateProfileRequest.getFirstName(),
                    updateProfileRequest.getLastName());

            // Create a simplified user response without sensitive data
            var userProfile = new java.util.HashMap<String, Object>();
            userProfile.put("id", user.getId());
            userProfile.put("username", user.getUsername());
            userProfile.put("firstName", user.getFirstName());
            userProfile.put("lastName", user.getLastName());
            userProfile.put("role", user.getRole());
            userProfile.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
