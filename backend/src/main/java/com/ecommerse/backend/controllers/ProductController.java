package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.dto.ProductVariantPositionRequest;
import com.ecommerse.backend.dto.ProductVariantRequest;
import com.ecommerse.backend.dto.ProductVariantResponse;
import com.ecommerse.backend.services.FileService;
import com.ecommerse.backend.services.ProductService;
import com.ecommerse.backend.services.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Product management
 * CRUD operations for Owner, read-only for Customer
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management operations")
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final FileService fileService;

    public ProductController(ProductService productService, ProductVariantService productVariantService,
            FileService fileService) {
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.fileService = fileService;
    }

    @Operation(summary = "Get all products", description = "Retrieve a paginated list of all active products. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "12") @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Sort field", example = "createdDate") @RequestParam(defaultValue = "createdDate") String sortBy,
            @Parameter(description = "Sort direction", example = "desc") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Category filter", example = "4") @RequestParam(name = "category", required = false) Long categoryId,
            @Parameter(description = "Search keyword") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Brand filter") @RequestParam(required = false) String brand,
            @Parameter(description = "In stock only") @RequestParam(defaultValue = "false") Boolean inStockOnly) {

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 50);

        java.util.Set<String> allowedSortFields = java.util.Set.of("createdDate", "price", "name", "id");
        String resolvedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "createdDate";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(resolvedSortBy).ascending()
                : Sort.by(resolvedSortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean useAdvanced = minPrice != null || maxPrice != null || (brand != null && !brand.trim().isEmpty())
                || Boolean.TRUE.equals(inStockOnly);
        if (useAdvanced) {
            Page<ProductDTO> products = productService.advancedSearch(normalizedSearch, categoryId, minPrice, maxPrice,
                    brand, inStockOnly, false, pageable);
            return ResponseEntity.ok(products);
        }

        Page<ProductDTO> products = productService.getCatalogProducts(pageable, categoryId, normalizedSearch);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get all brands", description = "Retrieve a list of distinct product brands. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved brands")
    })
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long id) {

        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get product by SKU", description = "Retrieve a specific product by its SKU. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDTO> getProductBySku(
            @Parameter(description = "Product SKU", example = "IPH15P-256-BLK", required = true) @PathVariable String sku) {

        return productService.getProductBySku(sku)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get products by category", description = "Retrieve products belonging to a specific category. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductDTO>> getProductsByCategory(
            @Parameter(description = "Category ID", example = "1", required = true) @PathVariable Long categoryId,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get featured products", description = "Retrieve all featured products. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved featured products")
    })
    @GetMapping("/featured")
    public ResponseEntity<List<ProductDTO>> getFeaturedProducts() {
        List<ProductDTO> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Search products", description = "Search products by name, description, or brand. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
            @Parameter(description = "Search term", example = "iPhone", required = true) @RequestParam String q,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.searchProducts(q.trim(), pageable);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get products by price range", description = "Retrieve products within a specific price range. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products"),
            @ApiResponse(responseCode = "400", description = "Invalid price range")
    })
    @GetMapping("/price-range")
    public ResponseEntity<Page<ProductDTO>> getProductsByPriceRange(
            @Parameter(description = "Minimum price", example = "100.00", required = true) @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", example = "1000.00", required = true) @RequestParam BigDecimal maxPrice,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {

        if (minPrice.compareTo(maxPrice) > 0) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(products);
    }

    // OWNER-ONLY OPERATIONS

    @Operation(summary = "Create new product", description = "Create a new product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "409", description = "Product with SKU already exists")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(
            @Parameter(description = "Product data", required = true) @Valid @RequestBody ProductDTO productDTO) {

        // DEBUG: Log authentication details
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== CREATE PRODUCT DEBUG ===");
        System.out.println("Authentication: " + (auth != null ? "present" : "null"));
        if (auth != null) {
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Name: " + auth.getName());
            System.out.println("Authorities: " + auth.getAuthorities());
            System.out.println("Is Authenticated: " + auth.isAuthenticated());
        }
        System.out.println("==========================");

        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(summary = "Update product", description = "Update an existing product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Product with SKU already exists")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "Updated product data", required = true) @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete product", description = "Soft delete a product (mark as inactive). Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long id) {

        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update product stock", description = "Update the stock quantity of a product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid stock quantity"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> updateStock(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "New stock quantity", example = "50", required = true) @RequestParam Integer quantity) {

        if (quantity < 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            productService.updateStock(id, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get low stock products", description = "Retrieve products with stock below threshold. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved low stock products"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role")
    })
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @Parameter(description = "Stock threshold", example = "10") @RequestParam(defaultValue = "10") Integer threshold) {

        List<ProductDTO> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Advanced product search", description = "Search products with multiple filters. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<ProductDTO>> advancedSearch(
            @Parameter(description = "Search term for name/description") @RequestParam(required = false) String query,
            @Parameter(description = "Category ID filter") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Brand filter") @RequestParam(required = false) String brand,
            @Parameter(description = "In stock only") @RequestParam(defaultValue = "false") Boolean inStockOnly,
            @Parameter(description = "Featured products only") @RequestParam(defaultValue = "false") Boolean featuredOnly,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "12") @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Sort field", example = "price") @RequestParam(defaultValue = "createdDate") String sortBy,
            @Parameter(description = "Sort direction", example = "asc") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductDTO> products = productService.advancedSearch(query, categoryId, minPrice, maxPrice,
                brand, inStockOnly, featuredOnly, pageable);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Bulk update products", description = "Update multiple products at once. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role")
    })
    @PutMapping("/bulk")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> bulkUpdateProducts(
            @Parameter(description = "List of products to update", required = true) @Valid @RequestBody List<ProductDTO> products) {

        try {
            List<ProductDTO> updatedProducts = productService.bulkUpdateProducts(products);
            return ResponseEntity.ok(updatedProducts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Bulk stock update", description = "Update stock for multiple products. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid stock data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role")
    })
    @PatchMapping("/bulk/stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> bulkUpdateStock(
            @Parameter(description = "Map of product ID to new stock quantity", required = true) @RequestBody Map<Long, Integer> stockUpdates) {

        try {
            productService.bulkUpdateStock(stockUpdates);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // VARIANT MANAGEMENT

    @Operation(summary = "List product variants", description = "Retrieve all variants for a product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variants retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId) {

        try {
            List<ProductVariantResponse> variants = productVariantService.listVariants(productId);
            return ResponseEntity.ok(variants);
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Create product variant", description = "Add a new variant under a product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Variant created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid variant data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductVariantResponse> createVariant(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "Variant data", required = true) @Valid @RequestBody ProductVariantRequest request) {

        try {
            ProductVariantResponse created = productVariantService.createVariant(productId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update product variant", description = "Modify an existing variant. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid variant data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found")
    })
    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "Variant ID", example = "10", required = true) @PathVariable Long variantId,
            @Parameter(description = "Updated variant data", required = true) @Valid @RequestBody ProductVariantRequest request) {

        try {
            ProductVariantResponse updated = productVariantService.updateVariant(productId, variantId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete product variant", description = "Remove a variant from a product. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Variant deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found")
    })
    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteVariant(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "Variant ID", example = "10", required = true) @PathVariable Long variantId) {

        try {
            productVariantService.deleteVariant(productId, variantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Set default product variant", description = "Mark a variant as the default selection. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant set as default"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found")
    })
    @PatchMapping("/{productId}/variants/{variantId}/default")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductVariantResponse> setDefaultVariant(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "Variant ID", example = "10", required = true) @PathVariable Long variantId) {

        try {
            ProductVariantResponse updated = productVariantService.setDefaultVariant(productId, variantId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Reorder product variants", description = "Update the display order of variants. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variants reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reorder payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{productId}/variants/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductVariantResponse>> reorderVariants(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "New variant positions", required = true) @Valid @RequestBody List<ProductVariantPositionRequest> positions) {

        try {
            List<ProductVariantResponse> reordered = productVariantService.reorderVariants(productId, positions);
            return ResponseEntity.ok(reordered);
        } catch (IllegalArgumentException e) {
            return isNotFound(e) ? ResponseEntity.notFound().build() : ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Upload variant image", description = "Upload an image for a specific variant. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "500", description = "File upload failed")
    })
    @PostMapping("/{productId}/variants/{variantId}/image")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadVariantImage(
            @Parameter(description = "Product ID", example = "1", required = true) @PathVariable Long productId,
            @Parameter(description = "Variant ID", example = "10", required = true) @PathVariable Long variantId,
            @Parameter(description = "Image file to upload", required = true) @RequestParam("file") MultipartFile file) {

        try {
            // Verify product and variant exist
            productVariantService.getVariant(productId, variantId);

            // Upload the image
            String relativePath = fileService.uploadProductImage(file);
            String fileUrl = fileService.getFileUrl(relativePath);

            // Update variant with image URL
            ProductVariantResponse variant = productVariantService.getVariant(productId, variantId);
            ProductVariantRequest updateRequest = new ProductVariantRequest();
            updateRequest.setName(variant.getName());
            updateRequest.setSku(variant.getSku());
            updateRequest.setPrice(variant.getPrice());
            updateRequest.setStockQuantity(variant.getStockQuantity());
            updateRequest.setActive(variant.getActive());
            updateRequest.setDefaultVariant(variant.getDefaultVariant());
            updateRequest.setOptions(variant.getOptions());
            updateRequest.setImageUrl(relativePath);

            productVariantService.updateVariant(productId, variantId, updateRequest);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", relativePath);
            response.put("fullUrl", fileUrl);
            response.put("message", "Variant image uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return isNotFound(e) ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private boolean isNotFound(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("not found");
    }
}
