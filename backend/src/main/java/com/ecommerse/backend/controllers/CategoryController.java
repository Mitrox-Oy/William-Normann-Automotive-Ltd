package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.CategoryDTO;
import com.ecommerse.backend.services.FileService;
import com.ecommerse.backend.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Category management
 * CRUD operations for Owner, read-only for Customer
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category management operations")
public class CategoryController {

    private final CategoryService categoryService;
    private final FileService fileService;

    public CategoryController(CategoryService categoryService, FileService fileService) {
        this.categoryService = categoryService;
        this.fileService = fileService;
    }

    @Operation(summary = "Get all categories", description = "Retrieve all active categories in tree structure. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    })
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get category by ID", description = "Retrieve a specific category by ID. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved category"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(
            @Parameter(description = "Category ID", example = "1", required = true) @PathVariable Long id) {

        Optional<CategoryDTO> category = categoryService.getCategoryById(id);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get category by slug", description = "Retrieve a specific category by slug. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved category"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryDTO> getCategoryBySlug(
            @Parameter(description = "Category slug", example = "parts", required = true) @PathVariable String slug) {

        Optional<CategoryDTO> category = categoryService.getCategoryBySlug(slug);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get root categories", description = "Retrieve top-level categories only. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved root categories")
    })
    @GetMapping("/root")
    public ResponseEntity<List<CategoryDTO>> getRootCategories() {
        List<CategoryDTO> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get category children", description = "Retrieve direct children of a category. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved child categories"),
            @ApiResponse(responseCode = "404", description = "Parent category not found")
    })
    @GetMapping("/{id}/children")
    public ResponseEntity<List<CategoryDTO>> getCategoryChildren(
            @Parameter(description = "Parent Category ID", example = "1", required = true) @PathVariable Long id) {

        List<CategoryDTO> children = categoryService.getCategoryChildren(id);
        return ResponseEntity.ok(children);
    }

    @Operation(summary = "Create new category", description = "Create a new category. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "409", description = "Category with name already exists")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryDTO> createCategory(
            @Parameter(description = "Category data", required = true) @Valid @RequestBody CategoryDTO categoryDTO) {

        try {
            CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update category", description = "Update an existing category. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @Parameter(description = "Category ID", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "Updated category data", required = true) @Valid @RequestBody CategoryDTO categoryDTO) {

        try {
            CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
            return ResponseEntity.ok(updatedCategory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete category", description = "Soft delete a category (mark as inactive). Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Category has products or children"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID", example = "1", required = true) @PathVariable Long id) {

        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Move category", description = "Move a category to a new parent. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category moved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid move operation (circular reference)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryDTO> moveCategory(
            @Parameter(description = "Category ID to move", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "New parent category ID (null for root)") @RequestParam(required = false) Long parentId) {

        try {
            CategoryDTO movedCategory = categoryService.moveCategory(id, parentId);
            return ResponseEntity.ok(movedCategory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Reorder categories", description = "Update sort order of categories. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category IDs or order"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role")
    })
    @PatchMapping("/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> reorderCategories(
            @Parameter(description = "List of category IDs in desired order", required = true) @RequestBody List<Long> categoryIds) {

        try {
            categoryService.reorderCategories(categoryIds);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Upload category image", description = "Upload an image for category create/edit forms. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "500", description = "File upload failed")
    })
    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadCategoryImage(
            @Parameter(description = "Image file to upload", required = true) @RequestParam("file") MultipartFile file) {
        try {
            String relativePath = fileService.uploadCategoryImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", relativePath);
            response.put("message", "Category image uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
