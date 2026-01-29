package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String username;

    private String firstName;

    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private User.Role role = User.Role.CUSTOMER; // Default to CUSTOMER if not specified

    // Constructors
    public RegisterRequest() {
    }

    public RegisterRequest(String username, String firstName, String lastName, String password, User.Role role) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.role = role != null ? role : User.Role.CUSTOMER;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User.Role getRole() {
        return role != null ? role : User.Role.CUSTOMER;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }
}
