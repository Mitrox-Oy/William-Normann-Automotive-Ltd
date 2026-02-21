package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductOcrFieldSuggestion;
import com.ecommerse.backend.dto.ProductOcrPrefillResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private ProductOcrPrefillResponse runOpenAiPrefill(MultipartFile file, String apiKey)
            throws IOException, InterruptedException {
        String model = env("OCR_OPENAI_MODEL", "gpt-5-mini");
        String fallbackModel = env("OCR_OPENAI_FALLBACK_MODEL", "gpt-4o-mini");
        int requestTimeoutSeconds = parseTimeoutSeconds(env("OCR_OPENAI_TIMEOUT_SECONDS", "22"));
        PreparedImage preparedImage = prepareImageForOcr(file);
        String mimeType = preparedImage.mimeType;
        String base64Image = preparedImage.base64;

        String instruction = "Extract automotive product details from the image and return JSON with this shape: " +
                "{\"rawText\":\"...\",\"fields\":{" +
                "\"name\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"price\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"brand\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"description\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"condition\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"productType\":{\"value\":\"car|part|tool|custom\",\"confidence\":0.0}," +
                "\"categorySlug\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"stockQuantity\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"quoteOnly\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"active\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"weight\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"make\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"model\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"year\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"mileage\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"powerKw\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"fuelType\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"transmission\":{\"value\":\"manual|automatic|semi-automatic\",\"confidence\":0.0}," +
                "\"bodyType\":{\"value\":\"sedan|coupe|suv|wagon|convertible|hatchback|van|truck\",\"confidence\":0.0},"
                +
                "\"driveType\":{\"value\":\"fwd|rwd|awd|4wd\",\"confidence\":0.0}," +
                "\"warrantyIncluded\":{\"value\":\"true|false\",\"confidence\":0.0}," +
                "\"partCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"partsDeepCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"partPosition\":{\"value\":\"front|rear|left|right|front-left|front-right|rear-left|rear-right\",\"confidence\":0.0},"
                +
                "\"wheelDiameterInch\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"wheelWidthInch\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"wheelBoltPattern\":{\"value\":\"e.g. 5x112, 5x120\",\"confidence\":0.0}," +
                "\"wheelOffsetEt\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"engineType\":{\"value\":\"e.g. inline-4, v6, v8\",\"confidence\":0.0}," +
                "\"engineDisplacementCc\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"enginePowerHp\":{\"value\":\"\",\"confidence\":0.0}" +
                "}}. " +
                "Rules: Use empty strings for unknown values. Confidence must be 0-1. " +
                "For wheels, extract diameter (e.g. 18), width (e.g. 8.5), bolt pattern (e.g. 5x112), offset/ET (e.g. 35). "
                +
                "For engines, extract type (inline-4/v6/v8), displacement in cc, power in hp. " +
                "For cars, extract bodyType (sedan/coupe/suv/etc), driveType (fwd/rwd/awd), transmission type. " +
                "For parts, identify position (front/rear/left/right) and category depth where visible.";

        String effectiveModel = model;
        OpenAiCallResult responsesResult;
        try {
            responsesResult = callResponsesApi(apiKey, effectiveModel, mimeType, base64Image, instruction,
                    requestTimeoutSeconds);
        } catch (IOException e) {
            if (isTimeoutException(e) && fallbackModel != null && !fallbackModel.isBlank()
                    && !fallbackModel.equalsIgnoreCase(model)) {
                effectiveModel = fallbackModel.trim();
                System.err.println("[OCR WARN] Primary model timed out, retrying with fallback model: " + effectiveModel);
                responsesResult = callResponsesApi(apiKey, effectiveModel, mimeType, base64Image, instruction,
                        requestTimeoutSeconds);
            } else {
                throw e;
            }
        }
        String content = "";
        String finalEndpoint = "responses";
        int finalStatusCode = responsesResult.statusCode;
        String finalErrorDetails = "";

        if (responsesResult.statusCode >= 200 && responsesResult.statusCode < 300) {
            JsonNode root = objectMapper.readTree(responsesResult.body);
            content = extractResponseContentFromResponses(root);
        } else {
            finalErrorDetails = extractOpenAiErrorDetails(responsesResult.body);
                OpenAiCallResult chatResult = callChatCompletionsApi(apiKey, effectiveModel, mimeType, base64Image, instruction,
                    requestTimeoutSeconds);
            finalEndpoint = "chat/completions";
            finalStatusCode = chatResult.statusCode;

            if (chatResult.statusCode >= 200 && chatResult.statusCode < 300) {
                JsonNode root = objectMapper.readTree(chatResult.body);
                content = root.path("choices").path(0).path("message").path("content").asText("");
            } else {
                String chatError = extractOpenAiErrorDetails(chatResult.body);
                String combinedError = finalErrorDetails;
                if (!chatError.isEmpty()) {
                    combinedError = (combinedError.isEmpty() ? "" : combinedError + " | ") + "fallback: " + chatError;
                }
                String message = "OCR provider error (" + finalStatusCode + ")";
                if (!combinedError.isEmpty()) {
                    message += ": " + combinedError;
                }
                System.err.println("[OCR ERROR] " + message + " | Model: " + effectiveModel + " | Endpoint: " + finalEndpoint);
                return notImplemented("openai", message + ".");
            }
        }

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

        private OpenAiCallResult callResponsesApi(String apiKey, String model, String mimeType, String base64Image,
            String instruction, int requestTimeoutSeconds) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("max_output_tokens", 900);
        payload.put("input", List.of(
                Map.of("role", "system", "content", List.of(
                        Map.of("type", "input_text",
                                "text", "You extract automotive product data from images. Return strict JSON only."))),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "input_text", "text", instruction),
                Map.of(
                    "type", "input_image",
                    "image_url", "data:" + mimeType + ";base64," + base64Image,
                    "detail", "low")))));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/responses"))
            .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new OpenAiCallResult(response.statusCode(), response.body());
    }

        private OpenAiCallResult callChatCompletionsApi(String apiKey, String model, String mimeType, String base64Image,
            String instruction, int requestTimeoutSeconds) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You extract automotive product data from images. Return strict JSON only."));

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(
                Map.of("type", "text", "text", instruction),
                Map.of("type", "image_url", "image_url",
                Map.of(
                    "url", "data:" + mimeType + ";base64," + base64Image,
                    "detail", "low"))));
        messages.add(userMessage);

        payload.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new OpenAiCallResult(response.statusCode(), response.body());
    }

    private String extractResponseContentFromResponses(JsonNode root) {
        String outputText = root.path("output_text").asText("");
        if (!outputText.isBlank()) {
            return outputText;
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        String type = contentItem.path("type").asText("");
                        if ("output_text".equals(type) || "text".equals(type)) {
                            String text = contentItem.path("text").asText("");
                            if (!text.isBlank()) {
                                return text;
                            }
                        }
                    }
                }
            }
        }

        return "";
    }

    private String extractOpenAiErrorDetails(String body) {
        try {
            JsonNode errorNode = objectMapper.readTree(body);
            String message = errorNode.path("error").path("message").asText("");
            return message.isBlank() ? body : message;
        } catch (Exception e) {
            return body;
        }
    }

    private int parseTimeoutSeconds(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(5, Math.min(29, parsed));
        } catch (Exception ignored) {
            return 22;
        }
    }

    private boolean isTimeoutException(IOException exception) {
        if (exception == null) {
            return false;
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("timed out") || exception.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("timeout");
    }

    private PreparedImage prepareImageForOcr(MultipartFile file) throws IOException {
        byte[] original = file.getBytes();
        int maxDimension = parseImageMaxDimension(env("OCR_IMAGE_MAX_DIMENSION", "1400"));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
        if (image == null) {
            String originalMimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            return new PreparedImage(originalMimeType, Base64.getEncoder().encodeToString(original));
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int largest = Math.max(width, height);
        if (largest <= maxDimension) {
            return new PreparedImage("image/jpeg", Base64.getEncoder().encodeToString(reencodeJpeg(image)));
        }

        double ratio = (double) maxDimension / (double) largest;
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        byte[] compressed = reencodeJpeg(scaled);
        return new PreparedImage("image/jpeg", Base64.getEncoder().encodeToString(compressed));
    }

    private byte[] reencodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private int parseImageMaxDimension(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(512, Math.min(2200, parsed));
        } catch (Exception ignored) {
            return 1400;
        }
    }

    private static class OpenAiCallResult {
        private final int statusCode;
        private final String body;

        private OpenAiCallResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static class PreparedImage {
        private final String mimeType;
        private final String base64;

        private PreparedImage(String mimeType, String base64) {
            this.mimeType = mimeType;
            this.base64 = base64;
        }
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
