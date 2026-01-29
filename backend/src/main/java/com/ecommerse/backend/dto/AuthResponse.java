package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.User;

public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private User.Role role;
    private String firstName;
    private String lastName;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String username, User.Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public AuthResponse(String token, Long id, String username, User.Role role, String firstName, String lastName) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
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
}
