package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OCR field suggestion with confidence")
public class ProductOcrFieldSuggestion {

    @Schema(description = "Suggested value", example = "BMW 330i M Sport")
    private String value;

    @Schema(description = "Confidence score in range 0..1", example = "0.91")
    private Double confidence;

    public ProductOcrFieldSuggestion() {
    }

    public ProductOcrFieldSuggestion(String value, Double confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
