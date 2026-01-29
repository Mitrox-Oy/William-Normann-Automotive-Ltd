package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.services.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Wishlist management
 * Customer wishlist functionality
 */
@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "Customer wishlist operations")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @Operation(summary = "Get user's wishlist", description = "Retrieve all products in user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved wishlist"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getWishlist(
            Authentication authentication,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {

        String username = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> wishlistProducts = wishlistService.getUserWishlist(username, pageable);
        return ResponseEntity.ok(wishlistProducts);
    }

    @Operation(summary = "Add product to wishlist", description = "Add a product to user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product added to wishlist successfully"),
            @ApiResponse(responseCode = "400", description = "Product already in wishlist"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> addToWishlist(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId) {

        String username = authentication.getName();
        try {
            boolean added = wishlistService.addToWishlist(username, productId);
            Map<String, Object> response = new HashMap<>();
            if (added) {
                response.put("message", "Product added to wishlist");
                response.put("inWishlist", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Product already in wishlist");
                response.put("inWishlist", true);
                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Remove product from wishlist", description = "Remove a product from user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product removed from wishlist successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role"),
            @ApiResponse(responseCode = "404", description = "Product not found in wishlist")
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId) {

        String username = authentication.getName();
        boolean removed = wishlistService.removeFromWishlist(username, productId);
        Map<String, Object> response = new HashMap<>();

        if (removed) {
            response.put("message", "Product removed from wishlist");
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Product not found in wishlist");
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(summary = "Check if product is in wishlist", description = "Check if a product is in user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId) {

        String username = authentication.getName();
        boolean inWishlist = wishlistService.isInWishlist(username, productId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("inWishlist", inWishlist);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Clear wishlist", description = "Remove all products from user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearWishlist(Authentication authentication) {
        String username = authentication.getName();
        int removedCount = wishlistService.clearWishlist(username);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wishlist cleared. Removed " + removedCount + " items.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get wishlist count", description = "Get the number of items in user's wishlist. Requires CUSTOMER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getWishlistCount(Authentication authentication) {
        String username = authentication.getName();
        long count = wishlistService.getWishlistCount(username);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}
