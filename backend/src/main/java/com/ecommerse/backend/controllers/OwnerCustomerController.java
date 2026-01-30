package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/customers")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class OwnerCustomerController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OwnerCustomerController(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public record CustomerSummary(
            Long id,
            String name,
            String email,
            String status,
            Long totalOrders,
            BigDecimal totalSpent,
            LocalDateTime createdAt,
            LocalDateTime lastOrderAt) {
    }

    public record UpdateCustomerStatusRequest(@NotBlank String status) {
    }

    @GetMapping
    public ResponseEntity<Page<CustomerSummary>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);

        // Load all customers (role-based)
        List<User> customers = userRepository.findByRole(User.Role.CUSTOMER);

        // Apply search filter
        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase(Locale.ROOT);
            customers = customers.stream()
                    .filter(u -> {
                        String fullName = (u.getFirstName() != null ? u.getFirstName() : "") + " "
                                + (u.getLastName() != null ? u.getLastName() : "");
                        return u.getUsername().toLowerCase(Locale.ROOT).contains(term)
                                || fullName.toLowerCase(Locale.ROOT).contains(term);
                    })
                    .collect(Collectors.toList());
        }

        // Apply status filter
        if (status != null && !status.isBlank()) {
            String normalized = status.toLowerCase(Locale.ROOT);
            customers = customers.stream()
                    .filter(u -> {
                        boolean active = u.isEnabled();
                        return ("active".equals(normalized) && active) || ("blocked".equals(normalized) && !active);
                    })
                    .collect(Collectors.toList());
        }

        int start = Math.min((int) pageable.getOffset(), customers.size());
        int end = Math.min(start + pageable.getPageSize(), customers.size());
        List<User> pageSlice = customers.subList(start, end);

        List<CustomerSummary> summaries = new ArrayList<>();
        for (User u : pageSlice) {
            summaries.add(toSummary(u));
        }

        Page<CustomerSummary> result = new PageImpl<>(summaries, pageable, customers.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || userOpt.get().getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toSummary(userOpt.get()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateCustomerStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || userOpt.get().getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String normalized = request.status().toLowerCase(Locale.ROOT);
        if ("blocked".equals(normalized)) {
            user.setEnabled(false);
        } else if ("active".equals(normalized)) {
            user.setEnabled(true);
        }

        userRepository.save(user);
        return ResponseEntity.ok(toSummary(user));
    }

    private CustomerSummary toSummary(User user) {
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        String name = !fullName.isBlank() ? fullName : user.getUsername();

        Long totalOrders = orderRepository.countByUser(user);
        BigDecimal totalSpent = orderRepository.sumTotalAmountByUser(user);
        LocalDateTime lastOrderAt = orderRepository.findLastOrderDateByUser(user);

        return new CustomerSummary(
                user.getId(),
                name,
                user.getUsername(),
                user.isEnabled() ? "active" : "blocked",
                totalOrders != null ? totalOrders : 0L,
                totalSpent != null ? totalSpent : BigDecimal.ZERO,
                user.getCreatedAt(),
                lastOrderAt);
    }
}
