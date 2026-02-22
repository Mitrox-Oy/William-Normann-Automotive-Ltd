package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductOcrFieldSuggestion;
import com.ecommerse.backend.dto.ProductOcrPrefillResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
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
        String model = env("OCR_OPENAI_MODEL", "gpt-5-nano");
        String fallbackModel = env("OCR_OPENAI_FALLBACK_MODEL", "gpt-4o-mini");
        String apiMode = parseApiMode(env("OCR_OPENAI_API_MODE", "auto"));
        String promptMode = parsePromptMode(env("OCR_OPENAI_PROMPT_MODE", "fast"));
        String responsesReasoningEffort = parseReasoningEffort(env("OCR_OPENAI_REASONING_EFFORT", ""), promptMode);
        int requestTimeoutSeconds = parseTimeoutSeconds(env("OCR_OPENAI_TIMEOUT_SECONDS", "22"));
        int maxOutputTokens = parseMaxOutputTokens(env("OCR_OPENAI_MAX_OUTPUT_TOKENS", "280"));
        boolean enableFallbackModel = parseBoolean(env("OCR_OPENAI_ENABLE_FALLBACK", "false"));
        String imageDetail = parseImageDetail(env("OCR_OPENAI_IMAGE_DETAIL", "low"));
        PreparedImage preparedImage = prepareImageForOcr(file);
        String mimeType = preparedImage.mimeType;
        String base64Image = Base64.getEncoder().encodeToString(preparedImage.bytes);

        String instruction = buildInstruction(promptMode);

        if ("chat".equals(apiMode)) {
            OcrContentResult chatResult = callChatWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens);
            if (chatResult.statusCode >= 200 && chatResult.statusCode < 300) {
                return parseSuccessfulOcr(chatResult.content, chatResult.body);
            }
            String error = extractOpenAiErrorDetails(chatResult.body);
            return notImplemented("openai", "OCR provider error (" + chatResult.statusCode + "): " + error + ".");
        }

        if ("responses".equals(apiMode)) {
            OcrContentResult responsesResult = callResponsesWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens, responsesReasoningEffort);
            if (responsesResult.statusCode >= 200 && responsesResult.statusCode < 300) {
                return parseSuccessfulOcr(responsesResult.content, responsesResult.body);
            }
            String error = extractOpenAiErrorDetails(responsesResult.body);
            return notImplemented("openai",
                    "OCR provider error (" + responsesResult.statusCode + "): " + error + ".");
        }

        OcrContentResult responsesResult = callResponsesWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens, responsesReasoningEffort);
        String content = responsesResult.content;
        String parseBody = responsesResult.body;
        String finalEndpoint = "responses";
        int finalStatusCode = responsesResult.statusCode;
        String finalErrorDetails = "";

        boolean shouldFallbackToChat = responsesResult.statusCode < 200 || responsesResult.statusCode >= 300
                || content.isBlank() || looksLikeIncompleteJson(content);
        if (shouldFallbackToChat) {
            finalErrorDetails = extractOpenAiErrorDetails(responsesResult.body);
            OcrContentResult chatResult = callChatWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens);
            finalEndpoint = "chat/completions";
            finalStatusCode = chatResult.statusCode;

            if (chatResult.statusCode >= 200 && chatResult.statusCode < 300) {
                content = chatResult.content;
                parseBody = chatResult.body;
            } else {
                String chatError = extractOpenAiErrorDetails(chatResult.body);
                String combinedError = finalErrorDetails;
                if (!chatError.isEmpty()) {
                    combinedError = (combinedError.isEmpty() ? "" : combinedError + " | ") + "fallback: " + chatError;
                }

                boolean shouldTryFallbackModel = enableFallbackModel && !fallbackModel.equals(model)
                        && (finalStatusCode == 408 || finalStatusCode == 429 || finalStatusCode == 500
                                || finalStatusCode == 502 || finalStatusCode == 503 || finalStatusCode == 504
                                || finalStatusCode == 598);

                if (shouldTryFallbackModel) {
                    OcrContentResult fallbackModelResult = callChatWithAdaptiveTokenRetry(apiKey, fallbackModel,
                            mimeType, base64Image, instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens);
                    if (fallbackModelResult.statusCode >= 200 && fallbackModelResult.statusCode < 300) {
                        content = fallbackModelResult.content;
                        parseBody = fallbackModelResult.body;
                    } else {
                        String fallbackModelError = extractOpenAiErrorDetails(fallbackModelResult.body);
                        if (!fallbackModelError.isBlank()) {
                            combinedError = (combinedError.isEmpty() ? "" : combinedError + " | ")
                                    + "fallback model " + fallbackModel + ": " + fallbackModelError;
                        }
                    }
                }

                if (!content.isBlank()) {
                    // fallback model succeeded
                } else {
                    String message = "OCR provider error (" + finalStatusCode + ")";
                    if (!combinedError.isEmpty()) {
                        message += ": " + combinedError;
                    }
                    System.err
                            .println("[OCR ERROR] " + message + " | Model: " + model + " | Endpoint: " + finalEndpoint);
                    return notImplemented("openai", message + ".");
                }
            }
        }

        return parseSuccessfulOcr(content, parseBody);
    }

    private ProductOcrPrefillResponse parseSuccessfulOcr(String content, String rawBody) {
        String resolvedContent = content;
        if ((resolvedContent == null || resolvedContent.isBlank()) && rawBody != null && !rawBody.isBlank()) {
            resolvedContent = extractJsonCandidateFromApiBody(rawBody);
        }

        if (resolvedContent == null || resolvedContent.isBlank()) {
            return notImplemented("openai", "OCR returned no content.");
        }

        JsonNode extracted = tryParseExtractionJson(unwrapJson(resolvedContent), rawBody);
        if (extracted == null) {
            return notImplemented("openai", "OCR response was incomplete. Please retry the scan.");
        }

        ProductOcrPrefillResponse result = new ProductOcrPrefillResponse();
        result.setImplemented(true);
        result.setProvider("openai");
        result.setMessage("OCR extraction completed. Review before saving.");
        result.setRawText(extracted.path("rawText").asText(""));
        result.setFields(parseFields(extracted.path("fields")));
        result.setMissingRequiredFields(resolveMissingRequired(result.getFields()));
        return result;
    }

    private JsonNode tryParseExtractionJson(String jsonText, String rawBody) {
        JsonNode parsed = tryParseExtractionNode(jsonText);
        if (parsed != null) {
            return parsed;
        }

        String balancedFromContent = extractBalancedJsonObject(jsonText);
        parsed = tryParseExtractionNode(balancedFromContent);
        if (parsed != null) {
            return parsed;
        }

        String fromRawBody = extractJsonCandidateFromApiBody(rawBody == null ? "" : rawBody);
        parsed = tryParseExtractionNode(fromRawBody);
        if (parsed != null) {
            return parsed;
        }

        String balancedFromRawBody = extractBalancedJsonObject(fromRawBody);
        return tryParseExtractionNode(balancedFromRawBody);
    }

    private JsonNode tryParseExtractionNode(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(candidate);
            if (node.isObject() && node.path("fields").isObject()) {
                return node;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private OpenAiCallResult callResponsesWithTimeoutHandling(String apiKey, String model, String mimeType,
            String base64Image, String instruction, int requestTimeoutSeconds, String imageDetail,
            int maxOutputTokens, String reasoningEffort) throws IOException, InterruptedException {
        try {
            return callResponsesApi(apiKey, model, mimeType, base64Image, instruction, requestTimeoutSeconds,
                    imageDetail, maxOutputTokens, reasoningEffort);
        } catch (HttpTimeoutException timeoutException) {
            return new OpenAiCallResult(598,
                    "{\"error\":{\"message\":\"responses timeout for model " + model + "\"}}");
        }
    }

    private OpenAiCallResult callChatWithTimeoutHandling(String apiKey, String model, String mimeType,
            String base64Image, String instruction, int requestTimeoutSeconds, String imageDetail,
            int maxOutputTokens) throws IOException, InterruptedException {
        try {
            return callChatCompletionsApi(apiKey, model, mimeType, base64Image, instruction, requestTimeoutSeconds,
                    imageDetail, maxOutputTokens);
        } catch (HttpTimeoutException timeoutException) {
            return new OpenAiCallResult(598,
                    "{\"error\":{\"message\":\"chat/completions timeout for model " + model + "\"}}");
        }
    }

    private OcrContentResult callResponsesWithAdaptiveTokenRetry(String apiKey, String model, String mimeType,
            String base64Image, String instruction, int requestTimeoutSeconds, String imageDetail, int maxOutputTokens,
            String reasoningEffort) throws IOException, InterruptedException {
        OpenAiCallResult firstResult = callResponsesWithTimeoutHandling(apiKey, model, mimeType, base64Image,
                instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens, reasoningEffort);
        OcrContentResult firstContentResult = parseResponsesContentResult(firstResult);
        if (firstResult.statusCode < 200 || firstResult.statusCode >= 300) {
            return firstContentResult;
        }

        if (!shouldRetryResponsesForTokenBudget(firstContentResult.root, firstContentResult.content, maxOutputTokens)) {
            return firstContentResult;
        }

        int retryMaxOutputTokens = computeAdaptiveRetryTokens(maxOutputTokens);
        OpenAiCallResult retryResult = callResponsesWithTimeoutHandling(apiKey, model, mimeType, base64Image,
                instruction, requestTimeoutSeconds, imageDetail, retryMaxOutputTokens, reasoningEffort);
        return parseResponsesContentResult(retryResult);
    }

    private OcrContentResult callChatWithAdaptiveTokenRetry(String apiKey, String model, String mimeType,
            String base64Image, String instruction, int requestTimeoutSeconds, String imageDetail, int maxOutputTokens)
            throws IOException, InterruptedException {
        OpenAiCallResult firstResult = callChatWithTimeoutHandling(apiKey, model, mimeType, base64Image, instruction,
                requestTimeoutSeconds, imageDetail, maxOutputTokens);
        OcrContentResult firstContentResult = parseChatContentResult(firstResult);
        if (firstResult.statusCode < 200 || firstResult.statusCode >= 300) {
            return firstContentResult;
        }

        if (!shouldRetryChatForTokenBudget(firstContentResult.root, firstContentResult.content, maxOutputTokens)) {
            return firstContentResult;
        }

        int retryMaxOutputTokens = computeAdaptiveRetryTokens(maxOutputTokens);
        OpenAiCallResult retryResult = callChatWithTimeoutHandling(apiKey, model, mimeType, base64Image, instruction,
                requestTimeoutSeconds, imageDetail, retryMaxOutputTokens);
        return parseChatContentResult(retryResult);
    }

    private OcrContentResult parseResponsesContentResult(OpenAiCallResult callResult) {
        JsonNode root = safeReadJson(callResult.body);
        String content = callResult.statusCode >= 200 && callResult.statusCode < 300
                ? extractResponseContentFromResponses(root)
                : "";
        return new OcrContentResult(callResult.statusCode, callResult.body, content, root);
    }

    private OcrContentResult parseChatContentResult(OpenAiCallResult callResult) {
        JsonNode root = safeReadJson(callResult.body);
        String content = callResult.statusCode >= 200 && callResult.statusCode < 300
                ? extractResponseContentFromChat(root)
                : "";
        return new OcrContentResult(callResult.statusCode, callResult.body, content, root);
    }

    private JsonNode safeReadJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private OpenAiCallResult callResponsesApi(String apiKey, String model, String mimeType, String base64Image,
            String instruction, int requestTimeoutSeconds, String imageDetail, int maxOutputTokens,
            String reasoningEffort)
            throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("max_output_tokens", maxOutputTokens);
        if (model != null && model.toLowerCase(Locale.ROOT).startsWith("gpt-5")) {
            payload.put("reasoning", Map.of("effort", reasoningEffort));
        }
        payload.put("input", List.of(
                Map.of("role", "system", "content", List.of(
                        Map.of("type", "input_text",
                                "text", "You extract automotive product data from images. Return strict JSON only."))),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "input_text", "text", instruction),
                        Map.of("type", "input_image", "image_url", "data:" + mimeType + ";base64," + base64Image,
                                "detail", imageDetail)))));

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
            String instruction, int requestTimeoutSeconds, String imageDetail, int maxOutputTokens)
            throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("max_completion_tokens", maxOutputTokens);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You extract automotive product data from images. Return strict JSON only."));

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(
                Map.of("type", "text", "text", instruction),
                Map.of("type", "image_url", "image_url",
                        Map.of("url", "data:" + mimeType + ";base64," + base64Image, "detail", imageDetail))));
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
                            String text = extractTextNode(contentItem.path("text"));
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

    private boolean shouldRetryResponsesForTokenBudget(JsonNode root, String content, int maxOutputTokens) {
        String incompleteReason = root.path("incomplete_details").path("reason").asText("");
        if ("max_output_tokens".equals(incompleteReason)) {
            return true;
        }

        int outputTokens = root.path("usage").path("output_tokens").asInt(-1);
        int reasoningTokens = root.path("usage").path("output_tokens_details").path("reasoning_tokens").asInt(-1);
        boolean hitLimit = outputTokens >= maxOutputTokens || reasoningTokens >= maxOutputTokens;
        return hitLimit && (content.isBlank() || looksLikeIncompleteJson(content));
    }

    private boolean shouldRetryChatForTokenBudget(JsonNode root, String content, int maxOutputTokens) {
        String finishReason = root.path("choices").path(0).path("finish_reason").asText("");
        if ("length".equals(finishReason)) {
            return true;
        }

        int completionTokens = root.path("usage").path("completion_tokens").asInt(-1);
        int reasoningTokens = root.path("usage").path("completion_tokens_details").path("reasoning_tokens").asInt(-1);
        boolean hitLimit = completionTokens >= maxOutputTokens || reasoningTokens >= maxOutputTokens;
        return hitLimit && (content.isBlank() || looksLikeIncompleteJson(content));
    }

    private int computeAdaptiveRetryTokens(int currentMaxOutputTokens) {
        int doubled = currentMaxOutputTokens * 2;
        return Math.min(1200, Math.max(900, doubled));
    }

    private String extractTextNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }
        if (node.isObject()) {
            String value = node.path("value").asText("");
            if (!value.isBlank()) {
                return value;
            }
            String text = node.path("text").asText("");
            if (!text.isBlank()) {
                return text;
            }
            return node.path("content").asText("");
        }
        return "";
    }

    private boolean looksLikeIncompleteJson(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = unwrapJson(text).trim();
        if (!normalized.contains("{")) {
            return false;
        }
        return extractBalancedJsonObject(normalized).isBlank();
    }

    private String extractBalancedJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int start = text.indexOf('{');
        if (start < 0) {
            return "";
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return "";
    }

    private String extractResponseContentFromChat(JsonNode root) {
        JsonNode message = root.path("choices").path(0).path("message");
        JsonNode contentNode = message.path("content");

        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }

        if (contentNode.isArray()) {
            StringBuilder aggregate = new StringBuilder();
            for (JsonNode item : contentNode) {
                String itemType = item.path("type").asText("");
                String text = "";
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    text = textNode.asText("");
                } else if (textNode.isObject()) {
                    text = textNode.path("value").asText("");
                }
                if (text.isBlank()) {
                    text = item.path("content").asText("");
                }
                if (text.isBlank() && item.path("content").isObject()) {
                    text = item.path("content").path("text").asText("");
                    if (text.isBlank()) {
                        text = item.path("content").path("value").asText("");
                    }
                }
                if (!text.isBlank() || "text".equals(itemType) || "output_text".equals(itemType)) {
                    if (!text.isBlank()) {
                        if (!aggregate.isEmpty()) {
                            aggregate.append('\n');
                        }
                        aggregate.append(text);
                    }
                }
            }
            if (!aggregate.isEmpty()) {
                return aggregate.toString();
            }
        }

        String legacyContent = root.path("choices").path(0).path("text").asText("");
        if (!legacyContent.isBlank()) {
            return legacyContent;
        }

        return "";
    }

    private String extractJsonCandidateFromApiBody(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode extractionNode = findExtractionNode(root);
            if (extractionNode != null) {
                return extractionNode.toString();
            }

            String embeddedJson = findEmbeddedJsonText(root);
            if (embeddedJson != null && !embeddedJson.isBlank()) {
                return embeddedJson;
            }
        } catch (Exception ignored) {
            // Fallback below
        }

        int fieldsIndex = body.indexOf("\"fields\"");
        if (fieldsIndex > 0) {
            int start = body.lastIndexOf('{', fieldsIndex);
            int end = body.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return body.substring(start, end + 1);
            }
        }

        return "";
    }

    private JsonNode findExtractionNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.isObject() && node.has("fields") && node.path("fields").isObject()) {
            return node;
        }

        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findExtractionNode(fields.next().getValue());
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findExtractionNode(child);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private String findEmbeddedJsonText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        if (node.isTextual()) {
            String text = node.asText("").trim();
            if (!text.isBlank() && text.contains("\"fields\"") && text.contains("{") && text.contains("}")) {
                return text;
            }
            return "";
        }

        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                String found = findEmbeddedJsonText(fields.next().getValue());
                if (!found.isBlank()) {
                    return found;
                }
            }
            return "";
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findEmbeddedJsonText(child);
                if (!found.isBlank()) {
                    return found;
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

    private String parseImageDetail(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("low".equals(normalized) || "high".equals(normalized) || "auto".equals(normalized)) {
            return normalized;
        }
        return "low";
    }

    private PreparedImage prepareImageForOcr(MultipartFile file) throws IOException {
        String inputMimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String normalizedMimeType = inputMimeType.toLowerCase(Locale.ROOT);
        byte[] originalBytes = file.getBytes();

        int maxDimension = parseDimension(env("OCR_OPENAI_IMAGE_MAX_DIMENSION", "1100"));
        long maxBytes = parseMaxBytes(env("OCR_OPENAI_IMAGE_MAX_BYTES", "700000"));

        BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (inputImage == null) {
            return new PreparedImage(originalBytes,
                    normalizedMimeType.startsWith("image/") ? normalizedMimeType : "image/jpeg");
        }

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        boolean needsResizeByDimensions = Math.max(width, height) > maxDimension;
        boolean needsCompressionBySize = originalBytes.length > maxBytes;

        if (!needsResizeByDimensions && !needsCompressionBySize) {
            return new PreparedImage(originalBytes,
                    normalizedMimeType.startsWith("image/") ? normalizedMimeType : "image/jpeg");
        }

        double scale = Math.min(1.0, (double) maxDimension / Math.max(width, height));

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = outputImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(inputImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        byte[] jpegBytes = writeJpeg(outputImage, 0.82f);
        if (jpegBytes.length > maxBytes) {
            jpegBytes = writeJpeg(outputImage, 0.68f);
        }

        return new PreparedImage(jpegBytes, "image/jpeg");
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            ImageOutputStream ios = ImageIO.createImageOutputStream(output);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
            ios.close();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private int parseDimension(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(600, Math.min(2200, parsed));
        } catch (Exception ignored) {
            return 1400;
        }
    }

    private long parseMaxBytes(String value) {
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(200_000L, Math.min(4_000_000L, parsed));
        } catch (Exception ignored) {
            return 900_000L;
        }
    }

    private static class PreparedImage {
        private final byte[] bytes;
        private final String mimeType;

        private PreparedImage(byte[] bytes, String mimeType) {
            this.bytes = bytes;
            this.mimeType = mimeType;
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

    private static class OcrContentResult {
        private final int statusCode;
        private final String body;
        private final String content;
        private final JsonNode root;

        private OcrContentResult(int statusCode, String body, String content, JsonNode root) {
            this.statusCode = statusCode;
            this.body = body;
            this.content = content == null ? "" : content;
            this.root = root;
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

    private String parseApiMode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("chat".equals(normalized) || "responses".equals(normalized) || "auto".equals(normalized)) {
            return normalized;
        }
        return "chat";
    }

    private String parsePromptMode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("fast".equals(normalized) || "full".equals(normalized)) {
            return normalized;
        }
        return "fast";
    }

    private String parseReasoningEffort(String value, String promptMode) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("minimal".equals(normalized) || "low".equals(normalized) || "medium".equals(normalized)
                || "high".equals(normalized)) {
            return normalized;
        }
        return "fast".equals(promptMode) ? "minimal" : "medium";
    }

    private int parseMaxOutputTokens(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(120, Math.min(1200, parsed));
        } catch (Exception ignored) {
            return 280;
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)
                || "on".equals(normalized);
    }

    private String buildInstruction(String promptMode) {
        if ("fast".equals(promptMode)) {
            return "Extract automotive product details from the image and return strict JSON only with shape: " +
                    "{\"rawText\":\"...\",\"fields\":{" +
                    "\"name\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"price\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"categorySlug\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"brand\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"description\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"productType\":{\"value\":\"car|part|tool|custom\",\"confidence\":0.0}," +
                    "\"make\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"model\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"year\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"mileage\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"powerKw\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"fuelType\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"transmission\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"bodyType\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"driveType\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"partCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"partsDeepCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"partPosition\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"wheelDiameterInch\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"wheelWidthInch\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"wheelBoltPattern\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"wheelOffsetEt\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"engineType\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"engineDisplacementCc\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"enginePowerHp\":{\"value\":\"\",\"confidence\":0.0}" +
                    "}}. Use empty strings for unknown values and confidence between 0 and 1.";
        }

        return "Extract automotive product details from the image and return JSON with this shape: " +
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
                "\"bodyType\":{\"value\":\"sedan|coupe|suv|wagon|convertible|hatchback|van|truck\",\"confidence\":0.0}," +
                "\"driveType\":{\"value\":\"fwd|rwd|awd|4wd\",\"confidence\":0.0}," +
                "\"warrantyIncluded\":{\"value\":\"true|false\",\"confidence\":0.0}," +
                "\"partCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"partsDeepCategory\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"partPosition\":{\"value\":\"front|rear|left|right|front-left|front-right|rear-left|rear-right\",\"confidence\":0.0}," +
                "\"wheelDiameterInch\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"wheelWidthInch\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"wheelBoltPattern\":{\"value\":\"e.g. 5x112, 5x120\",\"confidence\":0.0}," +
                "\"wheelOffsetEt\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"engineType\":{\"value\":\"e.g. inline-4, v6, v8\",\"confidence\":0.0}," +
                "\"engineDisplacementCc\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"enginePowerHp\":{\"value\":\"\",\"confidence\":0.0}" +
                "}}. Rules: Use empty strings for unknown values. Confidence must be 0-1. " +
                "For wheels, extract diameter (e.g. 18), width (e.g. 8.5), bolt pattern (e.g. 5x112), offset/ET (e.g. 35). " +
                "For engines, extract type (inline-4/v6/v8), displacement in cc, power in hp. " +
                "For cars, extract bodyType (sedan/coupe/suv/etc), driveType (fwd/rwd/awd), transmission type. " +
                "For parts, identify position (front/rear/left/right) and category depth where visible.";
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
