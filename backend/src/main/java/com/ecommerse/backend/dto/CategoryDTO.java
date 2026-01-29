package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Category
 */
@Schema(description = "Category information")
public class CategoryDTO {

    @Schema(description = "Category ID", example = "1")
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(description = "Category name", example = "Electronics", required = true)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Category description", example = "Electronic devices and gadgets")
    private String description;

    @Size(min = 2, max = 100, message = "Category slug must be between 2 and 100 characters")
    @Schema(description = "Category URL slug", example = "electronics")
    private String slug;

    @Schema(description = "Category image URL", example = "https://example.com/images/electronics.jpg")
    private String imageUrl;

    @Schema(description = "Whether the category is active", example = "true")
    private Boolean active;

    @Schema(description = "Category level in tree structure", example = "0")
    private Integer level;

    @Schema(description = "Sort order for display", example = "1")
    private Integer sortOrder;

    @Schema(description = "Parent category ID", example = "null")
    private Long parentId;

    @Schema(description = "Parent category name", example = "null")
    private String parentName;

    @Schema(description = "Child categories")
    private List<CategoryDTO> children;

    @Schema(description = "Whether category has children", example = "true")
    private Boolean hasChildren;

    @Schema(description = "Number of products in this category", example = "15")
    private Long productCount;

    @Schema(description = "Category creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Category last update date")
    private LocalDateTime updatedDate;

    // Constructors
    public CategoryDTO() {
    }

    public CategoryDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<CategoryDTO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryDTO> children) {
        this.children = children;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
