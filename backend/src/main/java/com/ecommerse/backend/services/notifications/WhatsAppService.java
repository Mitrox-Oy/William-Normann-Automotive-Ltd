package com.ecommerse.backend.services.notifications;

import com.ecommerse.backend.entities.Lead;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.api-base:https://graph.facebook.com}")
    private String apiBase;

    @Value("${whatsapp.api-version:v21.0}")
    private String apiVersion;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.recipient-number:+971585347970}")
    private String recipientNumber;

    public WhatsAppService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public SendResult sendLead(Lead lead) {
        if (!enabled) {
            return SendResult.failed("WhatsApp sending is disabled (whatsapp.enabled=false)");
        }

        String normalizedRecipient = normalizeRecipient(recipientNumber);
        if (normalizedRecipient.isBlank()) {
            return SendResult.failed("Recipient number is missing or invalid");
        }

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return SendResult.failed("whatsapp.phone-number-id is missing");
        }

        if (accessToken == null || accessToken.isBlank()) {
            return SendResult.failed("whatsapp.access-token is missing");
        }

        try {
            String url = String.format("%s/%s/%s/messages", trimTrailingSlash(apiBase), apiVersion.trim(),
                    phoneNumberId.trim());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", normalizedRecipient);
            payload.put("type", "text");
            payload.put("text", Map.of(
                    "preview_url", false,
                    "body", buildMessageBody(lead)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorText = extractError(response.body());
                logger.warn("WhatsApp API error {}: {}", response.statusCode(), errorText);
                return SendResult.failed("WhatsApp API error (" + response.statusCode() + "): " + errorText);
            }

            JsonNode root = objectMapper.readTree(response.body());
            String messageId = root.path("messages").path(0).path("id").asText("");
            if (messageId.isBlank()) {
                return SendResult.failed("WhatsApp accepted request but no message id returned");
            }

            return SendResult.sent(normalizedRecipient, messageId);
        } catch (Exception ex) {
            logger.error("Failed to send WhatsApp lead message: {}", ex.getMessage(), ex);
            return SendResult.failed(ex.getMessage());
        }
    }

    private String buildMessageBody(Lead lead) {
        StringBuilder builder = new StringBuilder();
        builder.append("🚗 New Quote Request\n");
        builder.append("Source: ").append(safe(lead.getSource())).append("\n");
        builder.append("Name: ").append(safe(lead.getName())).append("\n");
        builder.append("Email: ").append(safe(lead.getEmail())).append("\n");

        if (hasText(lead.getPhone())) {
            builder.append("Phone: ").append(lead.getPhone().trim()).append("\n");
        }
        if (hasText(lead.getInterest())) {
            builder.append("Interest: ").append(lead.getInterest().trim()).append("\n");
        }
        if (hasText(lead.getProductName())) {
            builder.append("Product: ").append(lead.getProductName().trim()).append("\n");
        }
        if (hasText(lead.getPartNumber())) {
            builder.append("Part Number: ").append(lead.getPartNumber().trim()).append("\n");
        }
        if (hasText(lead.getRequestedQuantity())) {
            builder.append("Quantity: ").append(lead.getRequestedQuantity().trim()).append("\n");
        }

        builder.append("Message:\n").append(safe(lead.getMessage())).append("\n");
        builder.append("Created: ").append(lead.getCreatedAt() != null
                ? lead.getCreatedAt().format(DATE_FORMATTER)
                : DATE_FORMATTER.format(java.time.LocalDateTime.now()));
        return builder.toString();
    }

    private String safe(String value) {
        return hasText(value) ? value.trim() : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return "";
        }
        String normalized = recipient.trim().replaceAll("[^0-9]", "");
        return normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://graph.facebook.com";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String extractError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String message = error.path("message").asText("");
                if (!message.isBlank()) {
                    return message;
                }
            }
        } catch (Exception ignored) {
        }

        String compact = responseBody == null ? "unknown error" : responseBody.trim().replaceAll("\\s+", " ");
        if (compact.length() > 400) {
            return compact.substring(0, 400);
        }
        return compact.isBlank() ? "unknown error" : compact;
    }

    public record SendResult(boolean sent, String recipient, String messageId, String error) {
        public static SendResult sent(String recipient, String messageId) {
            return new SendResult(true, recipient, messageId, null);
        }

        public static SendResult failed(String error) {
            return new SendResult(false, null, null, error);
        }
    }
}