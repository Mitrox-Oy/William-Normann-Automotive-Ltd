package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "OCR prefill extraction result")
public class ProductOcrPrefillResponse {

    @Schema(description = "Whether OCR integration is available and executed", example = "true")
    private boolean implemented;

    @Schema(description = "OCR provider used", example = "openai")
    private String provider;

    @Schema(description = "Result message")
    private String message;

    @Schema(description = "Best-effort raw extracted text")
    private String rawText;

    @Schema(description = "Field suggestions keyed by product field name")
    private Map<String, ProductOcrFieldSuggestion> fields = new LinkedHashMap<>();

    @Schema(description = "Required fields still missing after extraction")
    private List<String> missingRequiredFields = new ArrayList<>();

    public boolean isImplemented() {
        return implemented;
    }

    public void setImplemented(boolean implemented) {
        this.implemented = implemented;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Map<String, ProductOcrFieldSuggestion> getFields() {
        return fields;
    }

    public void setFields(Map<String, ProductOcrFieldSuggestion> fields) {
        this.fields = fields;
    }

    public List<String> getMissingRequiredFields() {
        return missingRequiredFields;
    }

    public void setMissingRequiredFields(List<String> missingRequiredFields) {
        this.missingRequiredFields = missingRequiredFields;
    }
}
