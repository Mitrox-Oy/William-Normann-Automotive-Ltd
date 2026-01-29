package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.CartAnalytics;
import com.ecommerse.backend.dto.CartDTO;
import com.ecommerse.backend.dto.CartItemDTO;
import com.ecommerse.backend.dto.CartValidationResult;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for cart operations
 */
@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get user's cart
     */
    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        Long userId = getCurrentUserId();
        try {
            CartDTO cart = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load cart", e);
        }
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addItemToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = getCurrentUserId();
        try {
            if (cartService.isCartFull(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Cart has reached maximum item limit");
                return ResponseEntity.badRequest().body(error);
            }

            CartItemDTO cartItem = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item added to cart successfully");
            response.put("cartItem", cartItem);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add item to cart", e);
        }
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Map<String, Object>> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        Long userId = getCurrentUserId();
        try {
            CartItemDTO cartItem = cartService.updateCartItemQuantity(userId, cartItemId, request.getQuantity());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item quantity updated successfully");
            response.put("cartItem", cartItem);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update item quantity", e);
        }
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Map<String, Object>> removeCartItem(@PathVariable Long cartItemId) {
        Long userId = getCurrentUserId();
        try {
            cartService.removeItemFromCart(userId, cartItemId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item removed from cart successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove item from cart", e);
        }
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCart() {
        Long userId = getCurrentUserId();
        try {
            cartService.clearCart(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart cleared successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to clear cart", e);
        }
    }

    /**
     * Validate cart
     */
    @GetMapping("/validate")
    public ResponseEntity<CartValidationResult> validateCart() {
        Long userId = getCurrentUserId();
        try {
            CartValidationResult validation = cartService.validateCartComprehensive(userId);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to validate cart", e);
        }
    }

    /**
     * Refresh cart (update prices and availability)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCart() {
        Long userId = getCurrentUserId();
        try {
            CartDTO cart = cartService.refreshCart(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart refreshed successfully");
            response.put("cart", cart);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh cart", e);
        }
    }

    /**
     * Update cart activity
     */
    @PostMapping("/activity")
    public ResponseEntity<Map<String, Object>> updateActivity() {
        Long userId = getCurrentUserId();
        try {
            cartService.updateCartActivity(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart activity updated");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update cart activity", e);
        }
    }

    /**
     * Get cart analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<CartAnalytics> getCartAnalytics() {
        Long userId = getCurrentUserId();
        try {
            CartAnalytics analytics = cartService.getCartAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load cart analytics", e);
        }
    }

    /**
     * Get cart item count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCartItemCount() {
        Long userId = getCurrentUserId();
        try {
            CartDTO cart = cartService.getCartByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("totalItems", cart.getTotalQuantity());
            response.put("uniqueItems", cart.getItemCount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("totalItems", 0);
            error.put("uniqueItems", 0);
            return ResponseEntity.ok(error);
        }
    }

    // Helper method to get current user ID from authentication
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user.getId();
        }

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        }

        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unsupported authentication principal");
    }

    // Request DTOs
    public static class AddToCartRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class UpdateQuantityRequest {
        private Integer quantity;

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
