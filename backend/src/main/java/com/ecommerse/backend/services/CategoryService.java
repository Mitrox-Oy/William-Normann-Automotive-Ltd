package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.CategoryDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing Categories with tree structure support
 */
@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Get all root categories (categories without parent)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndActiveTrue();
        return rootCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all categories with pagination
     */
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        Page<Category> categories = categoryRepository.findByActiveTrue(pageable);
        return categories.map(this::convertToDTO);
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public Optional<CategoryDTO> getCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .map(this::convertToDTO);
    }

    /**
     * Get category by slug
     */
    @Transactional(readOnly = true)
    public Optional<CategoryDTO> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlugAndActiveTrue(slug)
                .map(this::convertToDTO);
    }

    /**
     * Get child categories of a parent category
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildCategories(Long parentId) {
        List<Category> childCategories = categoryRepository.findByParentIdAndActiveTrue(parentId);
        return childCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get complete category tree starting from root
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndActiveTrue();
        return rootCategories.stream()
                .map(this::convertToDTOWithChildren)
                .collect(Collectors.toList());
    }

    /**
     * Get category path from root to the specified category
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryPath(Long categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findByIdAndActiveTrue(categoryId);
        if (categoryOpt.isEmpty()) {
            return List.of();
        }

        Category category = categoryOpt.get();
        List<Category> path = category.getPath();
        return path.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search categories by name
     */
    @Transactional(readOnly = true)
    public Page<CategoryDTO> searchCategories(String searchTerm, Pageable pageable) {
        Page<Category> categories = categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(searchTerm,
                pageable);
        return categories.map(this::convertToDTO);
    }

    /**
     * Create a new category
     */
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // Auto-generate slug if not provided
        if (categoryDTO.getSlug() == null || categoryDTO.getSlug().trim().isEmpty()) {
            categoryDTO.setSlug(generateSlug(categoryDTO.getName()));
        }

        // Validate parent category if specified
        if (categoryDTO.getParentId() != null) {
            Optional<Category> parent = categoryRepository.findByIdAndActiveTrue(categoryDTO.getParentId());
            if (parent.isEmpty()) {
                throw new IllegalArgumentException("Parent category not found with ID: " + categoryDTO.getParentId());
            }
        }

        // Check for duplicate slug
        if (categoryRepository.existsBySlugAndActiveTrue(categoryDTO.getSlug())) {
            // If slug exists, append a number
            categoryDTO.setSlug(generateUniqueSlug(categoryDTO.getSlug()));
        }

        Category category = convertToEntity(categoryDTO);
        category.setCreatedDate(LocalDateTime.now());
        category.setUpdatedDate(LocalDateTime.now());
        category.setActive(true);

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    /**
     * Update an existing category
     */
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        // Validate parent category if being changed
        if (categoryDTO.getParentId() != null && !categoryDTO.getParentId()
                .equals(existingCategory.getParent() != null ? existingCategory.getParent().getId() : null)) {
            // Check for circular reference
            if (categoryDTO.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            Optional<Category> newParent = categoryRepository.findByIdAndActiveTrue(categoryDTO.getParentId());
            if (newParent.isEmpty()) {
                throw new IllegalArgumentException("Parent category not found with ID: " + categoryDTO.getParentId());
            }

            // Check if new parent is not a descendant of current category
            if (existingCategory.isAncestorOf(newParent.get())) {
                throw new IllegalArgumentException("Cannot move category under its own descendant");
            }
        }

        // Check for duplicate slug (excluding current category)
        if (!existingCategory.getSlug().equals(categoryDTO.getSlug()) &&
                categoryRepository.existsBySlugAndActiveTrue(categoryDTO.getSlug())) {
            throw new IllegalArgumentException("Category already exists with slug: " + categoryDTO.getSlug());
        }

        // Update fields
        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setSlug(categoryDTO.getSlug());
        existingCategory.setImageUrl(categoryDTO.getImageUrl());
        existingCategory.setSortOrder(categoryDTO.getSortOrder());
        existingCategory.setUpdatedDate(LocalDateTime.now());

        // Update parent if changed
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndActiveTrue(categoryDTO.getParentId()).orElse(null);
            existingCategory.setParent(parent);
        } else {
            existingCategory.setParent(null);
        }

        Category updated = categoryRepository.save(existingCategory);
        return convertToDTO(updated);
    }

    /**
     * Soft delete a category (mark as inactive)
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        // Check if category has active children
        List<Category> activeChildren = categoryRepository.findByParentIdAndActiveTrue(id);
        if (!activeChildren.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete category with active child categories. Delete child categories first.");
        }

        // Check if category has active products
        // Note: This would require ProductRepository injection and checking
        // For now, we'll assume this check is done at the controller level

        category.setActive(false);
        category.setUpdatedDate(LocalDateTime.now());
        categoryRepository.save(category);
    }

    /**
     * Move category to a new parent
     */
    public CategoryDTO moveCategory(Long categoryId, Long newParentId) {
        Category category = categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        if (newParentId != null) {
            // Validate new parent exists
            Category newParent = categoryRepository.findByIdAndActiveTrue(newParentId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Parent category not found with ID: " + newParentId));

            // Check for circular reference
            if (newParentId.equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            // Check if new parent is not a descendant of current category
            if (category.isAncestorOf(newParent)) {
                throw new IllegalArgumentException("Cannot move category under its own descendant");
            }

            category.setParent(newParent);
        } else {
            category.setParent(null); // Move to root level
        }

        category.setUpdatedDate(LocalDateTime.now());
        Category updated = categoryRepository.save(category);
        return convertToDTO(updated);
    }

    /**
     * Get categories by level in the tree (0 = root, 1 = first level children,
     * etc.)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesByLevel(int level) {
        if (level == 0) {
            // For root categories, use the specific query
            List<Category> categories = categoryRepository.findRootCategoriesActive();
            return categories.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            // For deeper levels, we need to get all categories and filter by calculated
            // level
            List<Category> allCategories = categoryRepository.findByActiveTrueOrderBySortOrder();
            return allCategories.stream()
                    .filter(category -> category.getLevel() == level)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Convert Category entity to DTO
     */
    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSlug(category.getSlug());
        dto.setImageUrl(category.getImageUrl());
        dto.setSortOrder(category.getSortOrder());
        dto.setActive(category.isActive());
        dto.setLevel(category.getLevel());
        dto.setProductCount((long) category.getProducts().size());
        dto.setCreatedDate(category.getCreatedDate());
        dto.setUpdatedDate(category.getUpdatedDate());

        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }

        return dto;
    }

    /**
     * Convert Category entity to DTO with children populated
     */
    private CategoryDTO convertToDTOWithChildren(Category category) {
        CategoryDTO dto = convertToDTO(category);

        List<CategoryDTO> children = category.getChildren().stream()
                .filter(Category::isActive)
                .map(this::convertToDTOWithChildren)
                .collect(Collectors.toList());
        dto.setChildren(children);
        dto.setHasChildren(!children.isEmpty());

        return dto;
    }

    /**
     * Convert DTO to Category entity
     */
    private Category convertToEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSlug(dto.getSlug());
        category.setImageUrl(dto.getImageUrl());
        category.setSortOrder(dto.getSortOrder());

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndActiveTrue(dto.getParentId()).orElse(null);
            category.setParent(parent);
        }

        return category;
    }

    /**
     * Get all active categories (for CategoryController)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllActiveCategories() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get category children by parent ID (for CategoryController)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryChildren(Long parentId) {
        List<Category> children = categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId);
        return children.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Reorder categories by updating sort order
     */
    public void reorderCategories(List<Long> categoryIds) {
        for (int i = 0; i < categoryIds.size(); i++) {
            Long categoryId = categoryIds.get(i);
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                category.setSortOrder(i);
                category.setUpdatedDate(LocalDateTime.now());
                categoryRepository.save(category);
            }
        }
    }

    /**
     * Generate URL-friendly slug from category name
     */
    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "category";
        }

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Generate unique slug by appending numbers if slug already exists
     */
    private String generateUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;

        while (categoryRepository.existsBySlugAndActiveTrue(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
