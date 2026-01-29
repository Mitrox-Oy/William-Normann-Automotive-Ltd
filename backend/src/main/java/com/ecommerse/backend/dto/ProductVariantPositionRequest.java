package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating variant ordering.
 */
@Schema(description = "Variant ordering payload")
public class ProductVariantPositionRequest {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Variant identifier", example = "1001", required = true)
    private Long variantId;

    @NotNull(message = "Position is required")
    @Min(value = 0, message = "Position cannot be negative")
    @Schema(description = "Desired zero-based position", example = "0", required = true)
    private Integer position;

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
