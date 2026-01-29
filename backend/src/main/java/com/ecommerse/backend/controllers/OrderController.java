package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.FinalizeCheckoutRequest;
import com.ecommerse.backend.dto.OrderDTO;
import com.ecommerse.backend.dto.UpdateOrderStatusRequest;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderDTO>> getOrders(Authentication authentication,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersForUser(authentication.getName(), pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderDTO>> getAllOrders(Authentication authentication) {
        List<OrderDTO> orders = orderService.getOrdersForUser(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderDTO>> getOrdersForCurrentUser(Authentication authentication) {
        List<OrderDTO> orders = orderService.getOrdersForUser(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order detail by ID (customer only, verifies ownership)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id, Authentication authentication) {
        OrderDTO order = orderService.getOrderById(id, authentication.getName());
        return ResponseEntity.ok(order);
    }

    /**
     * Get order detail by Stripe Checkout session id (customer scope)
     */
    @GetMapping("/checkout-session/{sessionId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> getOrderByCheckoutSession(@PathVariable String sessionId,
                                                              Authentication authentication) {
        OrderDTO order = orderService.getOrderByCheckoutSession(sessionId, authentication.getName());
        return ResponseEntity.ok(order);
    }

    /**
     * Finalize checkout (customer fallback when returning from Stripe success page).
     */
    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> finalizeCheckout(
            @PathVariable Long id,
            @RequestBody(required = false) FinalizeCheckoutRequest request,
            Authentication authentication) {
        String sessionId = request != null ? request.getSessionId() : null;
        OrderDTO order = orderService.finalizeCheckout(id, sessionId, authentication.getName());
        return ResponseEntity.ok(order);
    }

    /**
     * Get the most recent order for the current customer
     */
    @GetMapping("/me/latest")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> getLatestOrderForCurrentUser(Authentication authentication) {
        OrderDTO order = orderService.getLatestOrderForUser(authentication.getName());
        return ResponseEntity.ok(order);
    }

    // ========== OWNER ENDPOINTS ==========

    /**
     * Get all orders for owner with filtering
     */
    @GetMapping({"/owner", "/owner/orders"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getOwnerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersForOwner(status, startDate, endDate, search, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order detail by ID for owner (no ownership verification)
     */
    @GetMapping("/owner/orders/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<OrderDTO> getOwnerOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderByIdForOwner(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Update order status
     */
    @PatchMapping("/owner/orders/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {
        OrderDTO order = orderService.updateOrderStatus(id, request.getStatus(), authentication.getName());
        return ResponseEntity.ok(order);
    }

    /**
     * Export orders to CSV
     */
    @GetMapping("/owner/orders/export")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<byte[]> exportOrdersCsv(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        String csvContent = orderService.exportOrdersToCsv(status, startDate, endDate);
        byte[] csvBytes = csvContent.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "orders_" + System.currentTimeMillis() + ".csv");
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}
