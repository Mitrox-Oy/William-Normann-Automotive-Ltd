package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.ProductImage;
import java.time.LocalDateTime;

public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private Integer position;
    private Boolean isMain;
    private Long productId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Default constructor
    public ProductImageResponse() {
    }

    // Constructor from ProductImage entity
    public ProductImageResponse(ProductImage productImage) {
        this.id = productImage.getId();
        this.imageUrl = productImage.getImageUrl();
        this.position = productImage.getPosition();
        this.isMain = productImage.getIsMain();
        this.productId = productImage.getProduct() != null ? productImage.getProduct().getId() : null;
        this.createdDate = productImage.getCreatedDate();
        this.updatedDate = productImage.getUpdatedDate();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getIsMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
