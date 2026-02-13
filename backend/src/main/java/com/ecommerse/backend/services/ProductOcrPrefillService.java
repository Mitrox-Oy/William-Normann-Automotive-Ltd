package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductOcrFieldSuggestion;
import com.ecommerse.backend.dto.ProductOcrPrefillResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductOcrPrefillService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ProductOcrPrefillService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public ProductOcrPrefillResponse prefillFromImage(MultipartFile file) {
        validateImage(file);

        String provider = env("OCR_PROVIDER", "openai").trim().toLowerCase(Locale.ROOT);
        if (!"openai".equals(provider)) {
            return notImplemented(provider, "OCR provider is not configured. Set OCR_PROVIDER=openai.");
        }

        String apiKey = env("OPENAI_API_KEY", "").trim();
        if (apiKey.isEmpty()) {
            return notImplemented("openai", "OPENAI_API_KEY is missing. Configure it to enable OCR prefill.");
        }

        try {
            return runOpenAiPrefill(file, apiKey);
        } catch (IOException | InterruptedException e) {
            ProductOcrPrefillResponse response = notImplemented("openai", "OCR request failed: " + e.getMessage());
            response.setImplemented(false);
            return response;
        }
    }

    private ProductOcrPrefillResponse runOpenAiPrefill(MultipartFile file, String apiKey) throws IOException, InterruptedException {
        String model = env("OCR_OPENAI_MODEL", "gpt-4o-mini");
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.1);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You extract automotive product data from images. Return strict JSON only."));

        String instruction = "Extract product details from the image and return JSON with this shape: " +
                "{\"rawText\":\"...\",\"fields\":{\"name\":{\"value\":\"\",\"confidence\":0.0},\"price\":{\"value\":\"\",\"confidence\":0.0},\"brand\":{\"value\":\"\",\"confidence\":0.0},\"description\":{\"value\":\"\",\"confidence\":0.0},\"condition\":{\"value\":\"\",\"confidence\":0.0},\"productType\":{\"value\":\"car|part|tool|custom\",\"confidence\":0.0},\"make\":{\"value\":\"\",\"confidence\":0.0},\"model\":{\"value\":\"\",\"confidence\":0.0},\"year\":{\"value\":\"\",\"confidence\":0.0},\"mileage\":{\"value\":\"\",\"confidence\":0.0},\"powerKw\":{\"value\":\"\",\"confidence\":0.0},\"fuelType\":{\"value\":\"\",\"confidence\":0.0},\"transmission\":{\"value\":\"\",\"confidence\":0.0},\"bodyType\":{\"value\":\"\",\"confidence\":0.0},\"driveType\":{\"value\":\"\",\"confidence\":0.0},\"stockQuantity\":{\"value\":\"\",\"confidence\":0.0},\"quoteOnly\":{\"value\":\"\",\"confidence\":0.0},\"active\":{\"value\":\"\",\"confidence\":0.0},\"weight\":{\"value\":\"\",\"confidence\":0.0},\"categorySlug\":{\"value\":\"\",\"confidence\":0.0}}}. " +
                "Use empty strings for unknown values. Confidence must be between 0 and 1.";

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(
                Map.of("type", "text", "text", instruction),
                Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image))
        ));
        messages.add(userMessage);

        payload.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return notImplemented("openai", "OCR provider error (" + response.statusCode() + ").");
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            return notImplemented("openai", "OCR returned no content.");
        }

        String jsonText = unwrapJson(content);
        JsonNode extracted = objectMapper.readTree(jsonText);

        ProductOcrPrefillResponse result = new ProductOcrPrefillResponse();
        result.setImplemented(true);
        result.setProvider("openai");
        result.setMessage("OCR extraction completed. Review before saving.");
        result.setRawText(extracted.path("rawText").asText(""));
        result.setFields(parseFields(extracted.path("fields")));
        result.setMissingRequiredFields(resolveMissingRequired(result.getFields()));

        return result;
    }

    private Map<String, ProductOcrFieldSuggestion> parseFields(JsonNode fieldsNode) {
        Map<String, ProductOcrFieldSuggestion> fields = new LinkedHashMap<>();
        if (fieldsNode == null || !fieldsNode.isObject()) {
            return fields;
        }

        fieldsNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode node = entry.getValue();
            String value = node.path("value").asText("").trim();
            double confidenceRaw = node.path("confidence").asDouble(0.0);
            double confidence = Math.max(0.0, Math.min(1.0, confidenceRaw));
            if (!value.isEmpty()) {
                fields.put(fieldName, new ProductOcrFieldSuggestion(value, confidence));
            }
        });

        return fields;
    }

    private List<String> resolveMissingRequired(Map<String, ProductOcrFieldSuggestion> fields) {
        List<String> missing = new ArrayList<>();
        if (!fields.containsKey("name")) {
            missing.add("name");
        }
        if (!fields.containsKey("price")) {
            missing.add("price");
        }
        if (!fields.containsKey("categorySlug")) {
            missing.add("categorySlug");
        }
        return missing;
    }

    private ProductOcrPrefillResponse notImplemented(String provider, String message) {
        ProductOcrPrefillResponse response = new ProductOcrPrefillResponse();
        response.setImplemented(false);
        response.setProvider(provider);
        response.setMessage(message);
        response.setRawText("");
        response.setFields(new LinkedHashMap<>());
        response.setMissingRequiredFields(List.of("name", "price", "categorySlug"));
        return response;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are supported for OCR prefill");
        }
    }

    private String unwrapJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            String withoutFence = trimmed.replaceFirst("^```(?:json)?", "");
            return withoutFence.substring(0, withoutFence.length() - 3).trim();
        }
        return trimmed;
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value != null ? value : fallback;
    }
}
