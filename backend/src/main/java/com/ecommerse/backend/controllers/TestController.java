package com.ecommerse.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("Public content - accessible to everyone");
    }
    
    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('OWNER')")
    public ResponseEntity<String> userAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok("User content for: " + auth.getName());
    }
    
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> customerAccess() {
        return ResponseEntity.ok("Customer Board - accessible only to customers");
    }
    
    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> ownerAccess() {
        return ResponseEntity.ok("Owner Board - accessible only to owners");
    }
}
