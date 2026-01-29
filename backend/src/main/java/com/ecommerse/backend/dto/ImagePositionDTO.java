package com.ecommerse.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ImagePositionDTO {
    @NotNull
    private Long imageId;

    @NotNull
    @Min(0)
    @Max(9)
    private Integer position;

    public ImagePositionDTO() {
    }

    public ImagePositionDTO(Long imageId, Integer position) {
        this.imageId = imageId;
        this.position = position;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
