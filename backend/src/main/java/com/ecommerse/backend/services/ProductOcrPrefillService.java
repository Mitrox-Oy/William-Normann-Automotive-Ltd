package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductOcrFieldSuggestion;
import com.ecommerse.backend.dto.ProductOcrPrefillResponse;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.repositories.CategoryRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductOcrPrefillService {

    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final HttpClient httpClient;

    private static final Set<String> ROOT_CATEGORY_SLUGS = Set.of("cars", "parts", "tools", "custom");
    private static final Pattern MILEAGE_HINT_PATTERN = Pattern.compile(
            "(?i)(?:mileage|odometer|odo|km|kilometer|kilometre|mi|miles?)\\D{0,20}([0-9][0-9\\s.,]{1,12})(?:\\s*([km]|mi|miles?))?");
    private static final Pattern GENERIC_UNIT_DISTANCE_PATTERN = Pattern
            .compile("(?i)([0-9][0-9\\s.,]{1,12})\\s*(km|kilometers?|kilometres?|mi|miles?)");
    private static final Pattern FLEX_NUMBER_PATTERN = Pattern.compile(
            "(?i)([-+]?[0-9]{1,3}(?:[\\s.,][0-9]{3})+|[-+]?[0-9]+(?:[.,][0-9]+)?)(?:\\s*([km]))?");

    public ProductOcrPrefillService(ObjectMapper objectMapper, CategoryRepository categoryRepository) {
        this.objectMapper = objectMapper;
        this.categoryRepository = categoryRepository;
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
        String apiMode = parseApiMode(env("OCR_OPENAI_API_MODE", "chat"));
        String promptMode = parsePromptMode(env("OCR_OPENAI_PROMPT_MODE", "fast"));
        String responsesReasoningEffort = parseReasoningEffort(env("OCR_OPENAI_REASONING_EFFORT", ""), promptMode);
        int requestTimeoutSeconds = parseTimeoutSeconds(env("OCR_OPENAI_TIMEOUT_SECONDS", "45"));
        int maxOutputTokens = parseMaxOutputTokens(env("OCR_OPENAI_MAX_OUTPUT_TOKENS", "700"));
        boolean enableFallbackModel = parseBoolean(env("OCR_OPENAI_ENABLE_FALLBACK", "true"));
        String imageDetail = parseImageDetail(env("OCR_OPENAI_IMAGE_DETAIL", "auto"));
        PreparedImage preparedImage = prepareImageForOcr(file);
        String mimeType = preparedImage.mimeType;
        String base64Image = Base64.getEncoder().encodeToString(preparedImage.bytes);

        String instruction = buildInstruction(promptMode);

        if ("chat".equals(apiMode)) {
            OcrContentResult chatResult = callChatWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens);
            if (chatResult.statusCode >= 200 && chatResult.statusCode < 300) {
                ProductOcrPrefillResponse parsedResult = parseSuccessfulOcr(chatResult.content, chatResult.body);
                if (isNoContentResponse(parsedResult)) {
                    return recoverNoContentExtraction(parsedResult, "chat", apiKey, model, fallbackModel, mimeType,
                            base64Image, requestTimeoutSeconds, imageDetail, maxOutputTokens,
                            responsesReasoningEffort, enableFallbackModel);
                }
                return parsedResult;
            }
            String error = extractOpenAiErrorDetails(chatResult.body);
            return notImplemented("openai", "OCR provider error (" + chatResult.statusCode + "): " + error + ".");
        }

        if ("responses".equals(apiMode)) {
            OcrContentResult responsesResult = callResponsesWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    instruction, requestTimeoutSeconds, imageDetail, maxOutputTokens, responsesReasoningEffort);
            if (responsesResult.statusCode >= 200 && responsesResult.statusCode < 300) {
                ProductOcrPrefillResponse parsedResult = parseSuccessfulOcr(responsesResult.content,
                        responsesResult.body);
                if (isNoContentResponse(parsedResult)) {
                    return recoverNoContentExtraction(parsedResult, "responses", apiKey, model, fallbackModel, mimeType,
                            base64Image, requestTimeoutSeconds, imageDetail, maxOutputTokens,
                            responsesReasoningEffort, enableFallbackModel);
                }
                return parsedResult;
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
                    int fallbackMaxTokens = Math.min(maxOutputTokens, 600);
                    String fallbackImageDetail = "low";
                    String fallbackInstruction = buildInstruction("fast");
                    OcrContentResult fallbackModelResult = callChatWithAdaptiveTokenRetry(apiKey, fallbackModel,
                            mimeType, base64Image, fallbackInstruction, requestTimeoutSeconds,
                            fallbackImageDetail, fallbackMaxTokens);
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

        ProductOcrPrefillResponse parsedResult = parseSuccessfulOcr(content, parseBody);
        if (!isNoContentResponse(parsedResult)) {
            return parsedResult;
        }

        return recoverNoContentExtraction(parsedResult, "auto", apiKey, model, fallbackModel, mimeType, base64Image,
                requestTimeoutSeconds, imageDetail, maxOutputTokens, responsesReasoningEffort, enableFallbackModel);
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
        Map<String, ProductOcrFieldSuggestion> parsedFields = parseFields(extracted.path("fields"));
        result.setFields(normalizeOcrFields(parsedFields, result.getRawText()));
        result.setMissingRequiredFields(resolveMissingRequired(result.getFields()));
        return result;
    }

    private boolean isNoContentResponse(ProductOcrPrefillResponse response) {
        if (response == null || response.isImplemented()) {
            return false;
        }
        String message = response.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("returned no content");
    }

    private ProductOcrPrefillResponse recoverNoContentExtraction(ProductOcrPrefillResponse originalResult,
            String primaryMode, String apiKey, String model, String fallbackModel, String mimeType, String base64Image,
            int requestTimeoutSeconds, String imageDetail, int maxOutputTokens, String responsesReasoningEffort,
            boolean enableFallbackModel)
            throws IOException, InterruptedException {
        String recoveryInstruction = buildInstruction("fast");
        int recoveryTokens = Math.min(maxOutputTokens, 900);

        if (!"responses".equals(primaryMode)) {
            OcrContentResult responsesRetry = callResponsesWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    recoveryInstruction, requestTimeoutSeconds, imageDetail, recoveryTokens, responsesReasoningEffort);
            if (responsesRetry.statusCode >= 200 && responsesRetry.statusCode < 300) {
                ProductOcrPrefillResponse parsed = parseSuccessfulOcr(responsesRetry.content, responsesRetry.body);
                if (parsed.isImplemented()) {
                    return parsed;
                }
            }
        }

        if (!"chat".equals(primaryMode)) {
            OcrContentResult chatRetry = callChatWithAdaptiveTokenRetry(apiKey, model, mimeType, base64Image,
                    recoveryInstruction, requestTimeoutSeconds, imageDetail, recoveryTokens);
            if (chatRetry.statusCode >= 200 && chatRetry.statusCode < 300) {
                ProductOcrPrefillResponse parsed = parseSuccessfulOcr(chatRetry.content, chatRetry.body);
                if (parsed.isImplemented()) {
                    return parsed;
                }
            }
        }

        if (enableFallbackModel && fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equals(model)) {
            int fallbackTokens = Math.min(recoveryTokens, 700);
            String fallbackImageDetail = "low";

            OcrContentResult chatFallback = callChatWithAdaptiveTokenRetry(apiKey, fallbackModel, mimeType, base64Image,
                    recoveryInstruction, requestTimeoutSeconds, fallbackImageDetail, fallbackTokens);
            if (chatFallback.statusCode >= 200 && chatFallback.statusCode < 300) {
                ProductOcrPrefillResponse parsed = parseSuccessfulOcr(chatFallback.content, chatFallback.body);
                if (parsed.isImplemented()) {
                    return parsed;
                }
            }

            OcrContentResult responsesFallback = callResponsesWithAdaptiveTokenRetry(apiKey, fallbackModel, mimeType,
                    base64Image, recoveryInstruction, requestTimeoutSeconds, fallbackImageDetail, fallbackTokens,
                    responsesReasoningEffort);
            if (responsesFallback.statusCode >= 200 && responsesFallback.statusCode < 300) {
                ProductOcrPrefillResponse parsed = parseSuccessfulOcr(responsesFallback.content, responsesFallback.body);
                if (parsed.isImplemented()) {
                    return parsed;
                }
            }
        }

        return originalResult;
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
        payload.put("text", Map.of("format", Map.of("type", "json_object")));
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
        payload.put("response_format", Map.of("type", "json_object"));

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
        return Math.min(2000, Math.max(1200, doubled));
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
            return Math.max(10, Math.min(120, parsed));
        } catch (Exception ignored) {
            return 45;
        }
    }

    private String parseImageDetail(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("low".equals(normalized) || "high".equals(normalized) || "auto".equals(normalized)) {
            return normalized;
        }
        return "auto";
    }

    private PreparedImage prepareImageForOcr(MultipartFile file) throws IOException {
        String inputMimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String normalizedMimeType = inputMimeType.toLowerCase(Locale.ROOT);
        byte[] originalBytes = file.getBytes();

        int maxDimension = parseDimension(env("OCR_OPENAI_IMAGE_MAX_DIMENSION", "1400"));
        long maxBytes = parseMaxBytes(env("OCR_OPENAI_IMAGE_MAX_BYTES", "1100000"));

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
            String fieldName = normalizeFieldKey(entry.getKey());
            JsonNode node = entry.getValue();
            String value = "";
            double confidenceRaw = 0.66;
            if (node.isObject()) {
                value = node.path("value").asText("").trim();
                if (value.isBlank()) {
                    value = node.path("text").asText("").trim();
                }
                if (value.isBlank()) {
                    value = node.path("content").asText("").trim();
                }
                confidenceRaw = node.path("confidence").asDouble(0.66);
            } else if (node.isValueNode()) {
                value = node.asText("").trim();
            }

            double confidence = clampConfidence(confidenceRaw);
            if (!value.isEmpty()) {
                ProductOcrFieldSuggestion existing = fields.get(fieldName);
                if (existing == null || existing.getConfidence() == null
                        || confidence >= existing.getConfidence()) {
                    fields.put(fieldName, new ProductOcrFieldSuggestion(value, confidence));
                }
            }
        });

        return fields;
    }

    private Map<String, ProductOcrFieldSuggestion> normalizeOcrFields(Map<String, ProductOcrFieldSuggestion> fields,
            String rawText) {
        Map<String, ProductOcrFieldSuggestion> normalized = new LinkedHashMap<>(fields);

        normalizeTokenField(normalized, "productType", this::normalizeProductTypeValue);
        normalizeTokenField(normalized, "condition", this::normalizeConditionValue);
        normalizeTokenField(normalized, "fuelType", this::normalizeFuelTypeValue);
        normalizeTokenField(normalized, "transmission", this::normalizeTransmissionValue);
        normalizeTokenField(normalized, "bodyType", this::normalizeBodyTypeValue);
        normalizeTokenField(normalized, "driveType", this::normalizeDriveTypeValue);
        normalizeTokenField(normalized, "oemType", this::normalizeOemTypeValue);
        normalizeTokenField(normalized, "powerSource", this::normalizePowerSourceValue);
        normalizeTokenField(normalized, "categorySlug", this::normalizeSlugToken);
        normalizeTokenField(normalized, "partsMainCategory", this::normalizeSlugToken);
        normalizeTokenField(normalized, "partsSubCategory", this::normalizeSlugToken);
        normalizeTokenField(normalized, "partsDeepCategory", this::normalizeSlugToken);
        normalizeTokenField(normalized, "partCategory", this::normalizeSlugToken);

        normalizeDecimalField(normalized, "price", 0.0, 100_000_000.0, true);
        normalizeDecimalField(normalized, "weight", 0.0, 10_000.0, true);
        normalizeDecimalField(normalized, "wheelDiameterInch", 1.0, 40.0, false);
        normalizeDecimalField(normalized, "wheelWidthInch", 1.0, 20.0, false);

        normalizeIntegerField(normalized, "stockQuantity", 0, 1_000_000, false, false);
        normalizeIntegerField(normalized, "year", 1885, 2100, false, false);
        normalizeIntegerField(normalized, "mileage", 0, 3_000_000, true, true);
        normalizeIntegerField(normalized, "powerKw", 0, 3000, false, false);
        normalizeIntegerField(normalized, "wheelOffsetEt", -300, 300, false, false);
        normalizeIntegerField(normalized, "engineDisplacementCc", 100, 20_000, false, false);
        normalizeIntegerField(normalized, "enginePowerHp", 1, 5000, false, false);

        String wheelBoltPattern = normalizeWheelBoltPattern(getFieldValue(normalized, "wheelBoltPattern"));
        if (!wheelBoltPattern.isBlank()) {
            upsertField(normalized, "wheelBoltPattern", wheelBoltPattern,
                    getFieldConfidence(normalized, "wheelBoltPattern", 0.7));
        }

        if (getFieldValue(normalized, "mileage").isBlank()) {
            String inferredMileage = extractMileageFromText(rawText);
            if (!inferredMileage.isBlank()) {
                upsertField(normalized, "mileage", inferredMileage, 0.58);
            }
        }

        String resolvedCategorySlug = resolveCategorySlug(normalized, rawText);
        if (!resolvedCategorySlug.isBlank()) {
            upsertField(normalized, "categorySlug", resolvedCategorySlug,
                    getFieldConfidence(normalized, "categorySlug", 0.62));
        }

        String partsMainFromCategory = inferPartsMainFromCategorySlug(getFieldValue(normalized, "categorySlug"));
        if (!partsMainFromCategory.isBlank() && getFieldValue(normalized, "partsMainCategory").isBlank()) {
            upsertField(normalized, "partsMainCategory", partsMainFromCategory, 0.6);
        }

        if (getFieldValue(normalized, "productType").isBlank()) {
            String categorySlug = getFieldValue(normalized, "categorySlug");
            if (categorySlug.startsWith("cars-") || "cars".equals(categorySlug)) {
                upsertField(normalized, "productType", "car", 0.56);
            } else if (categorySlug.startsWith("parts-") || "parts".equals(categorySlug)) {
                upsertField(normalized, "productType", "part", 0.56);
            } else if (categorySlug.startsWith("custom-") || "custom".equals(categorySlug)) {
                upsertField(normalized, "productType", "custom", 0.56);
            } else if (categorySlug.startsWith("tools-") || "tools".equals(categorySlug)) {
                upsertField(normalized, "productType", "tool", 0.56);
            }
        }

        return normalized;
    }

    private void normalizeTokenField(Map<String, ProductOcrFieldSuggestion> fields, String key,
            Function<String, String> normalizer) {
        String currentValue = getFieldValue(fields, key);
        if (currentValue.isBlank()) {
            return;
        }
        String normalizedValue = normalizer.apply(currentValue);
        if (!normalizedValue.isBlank()) {
            upsertField(fields, key, normalizedValue, getFieldConfidence(fields, key, 0.66));
        }
    }

    private void normalizeIntegerField(Map<String, ProductOcrFieldSuggestion> fields, String key, int min, int max,
            boolean allowSuffixMultiplier, boolean convertMilesToKm) {
        String currentValue = getFieldValue(fields, key);
        if (currentValue.isBlank()) {
            return;
        }
        String normalized = normalizeIntegerString(currentValue, min, max, allowSuffixMultiplier, convertMilesToKm);
        if (!normalized.isBlank()) {
            upsertField(fields, key, normalized, getFieldConfidence(fields, key, 0.66));
        }
    }

    private void normalizeDecimalField(Map<String, ProductOcrFieldSuggestion> fields, String key, double min, double max,
            boolean allowSuffixMultiplier) {
        String currentValue = getFieldValue(fields, key);
        if (currentValue.isBlank()) {
            return;
        }
        String normalized = normalizeDecimalString(currentValue, min, max, allowSuffixMultiplier);
        if (!normalized.isBlank()) {
            upsertField(fields, key, normalized, getFieldConfidence(fields, key, 0.66));
        }
    }

    private String getFieldValue(Map<String, ProductOcrFieldSuggestion> fields, String key) {
        ProductOcrFieldSuggestion suggestion = fields.get(key);
        if (suggestion == null || suggestion.getValue() == null) {
            return "";
        }
        return suggestion.getValue().trim();
    }

    private double getFieldConfidence(Map<String, ProductOcrFieldSuggestion> fields, String key, double fallback) {
        ProductOcrFieldSuggestion suggestion = fields.get(key);
        if (suggestion == null || suggestion.getConfidence() == null) {
            return clampConfidence(fallback);
        }
        return clampConfidence(suggestion.getConfidence());
    }

    private void upsertField(Map<String, ProductOcrFieldSuggestion> fields, String key, String value, double confidence) {
        if (value == null || value.isBlank()) {
            return;
        }
        ProductOcrFieldSuggestion existing = fields.get(key);
        if (existing == null) {
            fields.put(key, new ProductOcrFieldSuggestion(value.trim(), clampConfidence(confidence)));
            return;
        }
        existing.setValue(value.trim());
        double mergedConfidence = Math.max(getFieldConfidence(fields, key, 0.0), confidence);
        existing.setConfidence(clampConfidence(mergedConfidence));
    }

    private double clampConfidence(double confidenceRaw) {
        if (Double.isNaN(confidenceRaw) || Double.isInfinite(confidenceRaw)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, confidenceRaw));
    }

    private String normalizeFieldKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String normalized = key.trim();
        String lookup = normalized.toLowerCase(Locale.ROOT).replace("_", "");
        if ("category".equals(lookup) || "categoryname".equals(lookup) || "productcategory".equals(lookup)
                || "categoryslug".equals(lookup)) {
            return "categorySlug";
        }
        if ("miles".equals(lookup) || "odometer".equals(lookup)) {
            return "mileage";
        }
        if ("conditionnotes".equals(lookup)) {
            return "notesAndGrades";
        }
        if ("services".equals(lookup) || "servicehistory".equals(lookup)) {
            return "servicesHistory";
        }
        if ("partsmain".equals(lookup) || "partsmaincategory".equals(lookup)) {
            return "partsMainCategory";
        }
        if ("partssub".equals(lookup) || "partssubcategory".equals(lookup)) {
            return "partsSubCategory";
        }
        if ("partdeepcategory".equals(lookup)) {
            return "partsDeepCategory";
        }
        if ("wheeloffset".equals(lookup) || "offsetet".equals(lookup)) {
            return "wheelOffsetEt";
        }
        return normalized;
    }

    private String normalizeProductTypeValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("car", "cars", "vehicle", "vehicles", "auto", "automobile").contains(normalized)) {
            return "car";
        }
        if (Set.of("part", "parts", "sparepart", "spares", "component", "components").contains(normalized)) {
            return "part";
        }
        if (Set.of("tool", "tools").contains(normalized)) {
            return "tool";
        }
        if (Set.of("custom", "mod", "mods", "modification", "modifications").contains(normalized)) {
            return "custom";
        }
        return "";
    }

    private String normalizeConditionValue(String value) {
        String normalized = normalizeLookupToken(value);
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("1", "2", "3", "4", "5").contains(normalized)) {
            return normalized;
        }
        if ("one".equals(normalized)) {
            return "1";
        }
        if ("two".equals(normalized)) {
            return "2";
        }
        if ("three".equals(normalized)) {
            return "3";
        }
        if ("four".equals(normalized)) {
            return "4";
        }
        if ("five".equals(normalized)) {
            return "5";
        }
        if (Set.of("new", "used", "refurbished").contains(normalized)) {
            return normalized;
        }
        if ("refurb".equals(normalized) || "reconditioned".equals(normalized)) {
            return "refurbished";
        }
        return "";
    }

    private String normalizeFuelTypeValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "_");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("petrol", "gasoline", "gas").contains(normalized)) {
            return "petrol";
        }
        if ("diesel".equals(normalized)) {
            return "diesel";
        }
        if ("hybrid".equals(normalized)) {
            return "hybrid";
        }
        if (Set.of("ev", "electric").contains(normalized)) {
            return "electric";
        }
        if (Set.of("plugin_hybrid", "plug_in_hybrid", "phev").contains(normalized)) {
            return "plug_in_hybrid";
        }
        if ("other".equals(normalized)) {
            return "other";
        }
        return "";
    }

    private String normalizeTransmissionValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "_");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("manual", "mt").contains(normalized)) {
            return "manual";
        }
        if (Set.of("automatic", "auto", "at").contains(normalized)) {
            return "automatic";
        }
        if (Set.of("semi_automatic", "semi-auto", "automated_manual", "amt").contains(normalized)) {
            return "semi_automatic";
        }
        if (Set.of("dual_clutch", "dct").contains(normalized)) {
            return "dual_clutch";
        }
        if ("cvt".equals(normalized)) {
            return "cvt";
        }
        if ("other".equals(normalized)) {
            return "other";
        }
        return "";
    }

    private String normalizeBodyTypeValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "_");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("sedan", "saloon").contains(normalized)) {
            return "sedan";
        }
        if ("coupe".equals(normalized)) {
            return "coupe";
        }
        if (Set.of("hatchback", "hatch").contains(normalized)) {
            return "hatchback";
        }
        if (Set.of("wagon", "estate").contains(normalized)) {
            return "wagon";
        }
        if (Set.of("suv", "crossover").contains(normalized)) {
            return "suv";
        }
        if (Set.of("pickup", "truck").contains(normalized)) {
            return "pickup";
        }
        if (Set.of("van", "minivan", "mpv").contains(normalized)) {
            return "van";
        }
        if (Set.of("convertible", "cabrio", "roadster").contains(normalized)) {
            return "convertible";
        }
        if ("other".equals(normalized)) {
            return "other";
        }
        return "";
    }

    private String normalizeDriveTypeValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("fwd", "frontwheeldrive").contains(normalized)) {
            return "fwd";
        }
        if (Set.of("rwd", "rearwheeldrive").contains(normalized)) {
            return "rwd";
        }
        if (Set.of("awd", "allwheeldrive").contains(normalized)) {
            return "awd";
        }
        if (Set.of("4wd", "4x4", "fourwheeldrive").contains(normalized)) {
            return "4wd";
        }
        if ("other".equals(normalized)) {
            return "other";
        }
        return "";
    }

    private String normalizeOemTypeValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "");
        if (normalized.isBlank()) {
            return "";
        }
        if (Set.of("oem", "genuine").contains(normalized)) {
            return "oem";
        }
        if (Set.of("aftermarket", "aftermarketpart", "aftermarketparts").contains(normalized)) {
            return "aftermarket";
        }
        return "";
    }

    private String normalizePowerSourceValue(String value) {
        String normalized = normalizeLookupToken(value).replace(" ", "_");
        if (normalized.isBlank()) {
            return "";
        }
        if ("manual".equals(normalized)) {
            return "manual";
        }
        if (Set.of("electric", "electric_corded", "corded", "mains").contains(normalized)) {
            return "electric_corded";
        }
        if (Set.of("battery", "cordless").contains(normalized)) {
            return "battery";
        }
        if ("pneumatic".equals(normalized)) {
            return "pneumatic";
        }
        if ("hydraulic".equals(normalized)) {
            return "hydraulic";
        }
        if ("other".equals(normalized)) {
            return "other";
        }
        return "";
    }

    private String normalizeSlugToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .trim()
                .replace("_", "-")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private String normalizeWheelBoltPattern(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("*", "x")
                .replaceAll("[^0-9x]", "")
                .replaceAll("xx+", "x");
        if (normalized.matches("\\d{3}")) {
            return normalized.charAt(0) + "x" + normalized.substring(1);
        }
        if (normalized.matches("\\d+x\\d{2,3}")) {
            return normalized;
        }
        return "";
    }

    private String normalizeIntegerString(String value, int min, int max, boolean allowSuffixMultiplier,
            boolean convertMilesToKm) {
        BigDecimal parsed = parseFlexibleNumber(value, allowSuffixMultiplier, convertMilesToKm);
        if (parsed == null) {
            return "";
        }
        try {
            int resolved = parsed.setScale(0, RoundingMode.HALF_UP).intValueExact();
            if (resolved < min || resolved > max) {
                return "";
            }
            return String.valueOf(resolved);
        } catch (ArithmeticException ex) {
            return "";
        }
    }

    private String normalizeDecimalString(String value, double min, double max, boolean allowSuffixMultiplier) {
        BigDecimal parsed = parseFlexibleNumber(value, allowSuffixMultiplier, false);
        if (parsed == null) {
            return "";
        }
        BigDecimal minValue = BigDecimal.valueOf(min);
        BigDecimal maxValue = BigDecimal.valueOf(max);
        if (parsed.compareTo(minValue) < 0 || parsed.compareTo(maxValue) > 0) {
            return "";
        }
        return parsed.stripTrailingZeros().toPlainString();
    }

    private BigDecimal parseFlexibleNumber(String rawValue, boolean allowSuffixMultiplier, boolean convertMilesToKm) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Matcher matcher = FLEX_NUMBER_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }

        String numericPart = matcher.group(1);
        String suffix = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
        String normalizedLiteral = normalizeNumericLiteral(numericPart);
        if (normalizedLiteral.isBlank()) {
            return null;
        }

        BigDecimal resolved;
        try {
            resolved = new BigDecimal(normalizedLiteral);
        } catch (NumberFormatException ex) {
            return null;
        }

        if (allowSuffixMultiplier) {
            if ("k".equals(suffix)) {
                resolved = resolved.multiply(BigDecimal.valueOf(1000));
            } else if ("m".equals(suffix)) {
                resolved = resolved.multiply(BigDecimal.valueOf(1_000_000));
            }
        }

        if (convertMilesToKm) {
            String lowercaseRaw = rawValue.toLowerCase(Locale.ROOT);
            boolean explicitMiles = lowercaseRaw.contains("mile") || lowercaseRaw.matches(".*\\bmi\\b.*");
            if (explicitMiles) {
                resolved = resolved.multiply(BigDecimal.valueOf(1.609344));
            }
        }

        return resolved;
    }

    private String normalizeNumericLiteral(String literal) {
        if (literal == null || literal.isBlank()) {
            return "";
        }

        String cleaned = literal.trim().replace(" ", "");
        boolean negative = cleaned.startsWith("-");
        if (negative || cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.isBlank()) {
            return "";
        }

        int commaCount = countChar(cleaned, ',');
        int dotCount = countChar(cleaned, '.');
        if (commaCount > 0 && dotCount > 0) {
            int lastComma = cleaned.lastIndexOf(',');
            int lastDot = cleaned.lastIndexOf('.');
            if (lastComma > lastDot) {
                cleaned = cleaned.replace(".", "");
                cleaned = cleaned.replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (commaCount > 0) {
            if (looksLikeThousandsSeparated(cleaned, ',')) {
                cleaned = cleaned.replace(",", "");
            } else if (commaCount == 1 && cleaned.length() - cleaned.lastIndexOf(',') - 1 <= 2) {
                cleaned = cleaned.replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (dotCount > 0) {
            if (looksLikeThousandsSeparated(cleaned, '.')) {
                cleaned = cleaned.replace(".", "");
            } else if (dotCount > 1) {
                cleaned = cleaned.replace(".", "");
            }
        }

        cleaned = cleaned.replaceAll("[^0-9.]", "");
        if (cleaned.isBlank() || ".".equals(cleaned)) {
            return "";
        }
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return negative ? "-" + cleaned : cleaned;
    }

    private boolean looksLikeThousandsSeparated(String value, char separator) {
        String escapedSeparator = Pattern.quote(String.valueOf(separator));
        String[] parts = value.split(escapedSeparator);
        if (parts.length < 2 || parts[0].isBlank() || parts[0].length() > 3) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() != 3) {
                return false;
            }
        }
        return true;
    }

    private int countChar(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private String resolveCategorySlug(Map<String, ProductOcrFieldSuggestion> fields, String rawText) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        if (categories.isEmpty()) {
            return normalizeSlugToken(getFieldValue(fields, "categorySlug"));
        }

        String productType = normalizeProductTypeValue(getFieldValue(fields, "productType"));
        String providedCategory = getFieldValue(fields, "categorySlug");

        String resolved = findCategoryByExactSlug(providedCategory, productType, categories);
        if (resolved.isBlank()) {
            resolved = findCategoryByName(providedCategory, categories);
        }
        if (resolved.isBlank() && !providedCategory.isBlank()) {
            resolved = findCategoryByLooseToken(providedCategory, categories, productType);
        }
        if (resolved.isBlank()) {
            resolved = inferCategoryFromHeuristics(fields, rawText, categories, productType);
        }
        return fallbackNonRootForRootCategory(resolved, categories, productType);
    }

    private String findCategoryByExactSlug(String candidate, String productType, List<Category> categories) {
        String normalizedCandidate = normalizeSlugToken(candidate);
        if (!normalizedCandidate.isBlank()) {
            for (Category category : categories) {
                if (normalizedCandidate.equalsIgnoreCase(category.getSlug())) {
                    return category.getSlug();
                }
            }
        }

        if (normalizedCandidate.isBlank() || productType.isBlank()) {
            return "";
        }

        String prefix = "";
        if ("car".equals(productType)) {
            prefix = "cars";
        } else if ("part".equals(productType)) {
            prefix = "parts";
        } else if ("tool".equals(productType)) {
            prefix = "tools";
        } else if ("custom".equals(productType)) {
            prefix = "custom";
        }
        if (prefix.isBlank()) {
            return "";
        }

        String prefixedSlug = prefix + "-" + normalizedCandidate;
        for (Category category : categories) {
            if (prefixedSlug.equalsIgnoreCase(category.getSlug())) {
                return category.getSlug();
            }
        }
        return "";
    }

    private String findCategoryByName(String candidate, List<Category> categories) {
        String normalizedCandidate = normalizeLookupToken(candidate);
        if (normalizedCandidate.isBlank()) {
            return "";
        }
        for (Category category : categories) {
            if (normalizedCandidate.equals(normalizeLookupToken(category.getName()))) {
                return category.getSlug();
            }
        }
        return "";
    }

    private String findCategoryByLooseToken(String candidate, List<Category> categories, String productType) {
        String normalizedCandidate = normalizeLookupToken(candidate);
        if (normalizedCandidate.isBlank() || normalizedCandidate.length() < 3) {
            return "";
        }

        String requiredPrefix = "";
        if ("car".equals(productType)) {
            requiredPrefix = "cars-";
        } else if ("part".equals(productType)) {
            requiredPrefix = "parts-";
        } else if ("tool".equals(productType)) {
            requiredPrefix = "tools-";
        } else if ("custom".equals(productType)) {
            requiredPrefix = "custom-";
        }

        int bestScore = 0;
        String bestSlug = "";
        for (Category category : categories) {
            String slug = category.getSlug() == null ? "" : category.getSlug().toLowerCase(Locale.ROOT);
            if (!requiredPrefix.isBlank() && !slug.startsWith(requiredPrefix)) {
                continue;
            }

            String normalizedName = normalizeLookupToken(category.getName());
            int score = 0;
            if (slug.contains(normalizedCandidate.replace(" ", "-"))) {
                score += 2;
            }
            if (normalizedName.contains(normalizedCandidate)) {
                score += 2;
            }
            if (normalizedCandidate.contains(normalizedName) && normalizedName.length() >= 3) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlug = category.getSlug();
            }
        }
        return bestScore >= 2 ? bestSlug : "";
    }

    private String inferCategoryFromHeuristics(Map<String, ProductOcrFieldSuggestion> fields, String rawText,
            List<Category> categories, String productType) {
        String partsMainCategory = normalizeSlugToken(getFieldValue(fields, "partsMainCategory"));
        String partCategory = normalizeLookupToken(getFieldValue(fields, "partCategory"));
        String partsDeepCategory = normalizeLookupToken(getFieldValue(fields, "partsDeepCategory"));
        String make = normalizeLookupToken(getFieldValue(fields, "make"));

        Set<String> hints = new LinkedHashSet<>();
        hints.add(normalizeLookupToken(getFieldValue(fields, "name")));
        hints.add(partCategory);
        hints.add(partsDeepCategory);
        hints.add(normalizeLookupToken(rawText));
        String combinedHints = String.join(" ", hints);

        if ("part".equals(productType) || partsMainCategory.startsWith("engine")
                || partsMainCategory.startsWith("wheel")) {
            if (partsMainCategory.startsWith("wheel")
                    || combinedHints.contains("wheel")
                    || combinedHints.contains("rim")
                    || combinedHints.contains("tire")
                    || combinedHints.contains("tyre")) {
                String wheels = findCategoryByExactSlug("parts-wheels", productType, categories);
                if (!wheels.isBlank()) {
                    return wheels;
                }
            }
            if (partsMainCategory.startsWith("engine")
                    || combinedHints.contains("engine")
                    || combinedHints.contains("turbo")
                    || combinedHints.contains("drivetrain")
                    || combinedHints.contains("transmission")) {
                String engineDrivetrain = findCategoryByExactSlug("parts-engine-drivetrain", productType, categories);
                if (!engineDrivetrain.isBlank()) {
                    return engineDrivetrain;
                }
            }
            String others = findCategoryByExactSlug("parts-others", productType, categories);
            if (!others.isBlank()) {
                return others;
            }
        }

        if ("car".equals(productType)) {
            if (!make.isBlank()) {
                for (Category category : categories) {
                    String slug = category.getSlug() == null ? "" : category.getSlug().toLowerCase(Locale.ROOT);
                    if (slug.startsWith("cars-") && slug.contains(make.replace(" ", "-"))) {
                        return category.getSlug();
                    }
                }
            }
            if (combinedHints.contains("jdm")) {
                String jdm = findCategoryByExactSlug("cars-jdm", productType, categories);
                if (!jdm.isBlank()) {
                    return jdm;
                }
            }
            if (combinedHints.contains("euro")) {
                String euro = findCategoryByExactSlug("cars-euro-spec", productType, categories);
                if (!euro.isBlank()) {
                    return euro;
                }
                euro = findCategoryByExactSlug("cars-euro", productType, categories);
                if (!euro.isBlank()) {
                    return euro;
                }
            }
            if (combinedHints.contains("luxury")) {
                String luxury = findCategoryByExactSlug("cars-luxury", productType, categories);
                if (!luxury.isBlank()) {
                    return luxury;
                }
            }
            if (combinedHints.contains("super car") || combinedHints.contains("supercar")) {
                String superCars = findCategoryByExactSlug("cars-super-cars", productType, categories);
                if (!superCars.isBlank()) {
                    return superCars;
                }
            }
        }

        return fallbackCategoryByProductType(productType, categories);
    }

    private String fallbackCategoryByProductType(String productType, List<Category> categories) {
        String prefix = "";
        if ("car".equals(productType)) {
            prefix = "cars-";
        } else if ("part".equals(productType)) {
            prefix = "parts-";
        } else if ("tool".equals(productType)) {
            prefix = "tools-";
        } else if ("custom".equals(productType)) {
            prefix = "custom-";
        }
        if (prefix.isBlank()) {
            return "";
        }
        for (Category category : categories) {
            String slug = category.getSlug() == null ? "" : category.getSlug().toLowerCase(Locale.ROOT);
            if (slug.startsWith(prefix)) {
                return category.getSlug();
            }
        }
        return "";
    }

    private String fallbackNonRootForRootCategory(String resolvedSlug, List<Category> categories, String productType) {
        if (resolvedSlug == null || resolvedSlug.isBlank()) {
            return "";
        }
        String normalized = resolvedSlug.toLowerCase(Locale.ROOT);
        if (!ROOT_CATEGORY_SLUGS.contains(normalized)) {
            return resolvedSlug;
        }

        if ("parts".equals(normalized)) {
            String preferred = findCategoryByExactSlug("parts-others", "part", categories);
            if (!preferred.isBlank()) {
                return preferred;
            }
        } else if ("cars".equals(normalized)) {
            String preferred = findCategoryByExactSlug("cars-jdm", "car", categories);
            if (!preferred.isBlank()) {
                return preferred;
            }
        } else if ("tools".equals(normalized)) {
            String preferred = findCategoryByExactSlug("tools-hand", "tool", categories);
            if (!preferred.isBlank()) {
                return preferred;
            }
        } else if ("custom".equals(normalized)) {
            String preferred = findCategoryByExactSlug("custom-performance", "custom", categories);
            if (!preferred.isBlank()) {
                return preferred;
            }
        }

        String fallback = fallbackCategoryByProductType(productType, categories);
        if (!fallback.isBlank()) {
            return fallback;
        }
        return resolvedSlug;
    }

    private String inferPartsMainFromCategorySlug(String categorySlug) {
        String normalized = normalizeSlugToken(categorySlug);
        if (normalized.isBlank() || (!"parts".equals(normalized) && !normalized.startsWith("parts-"))) {
            return "";
        }
        if (normalized.contains("engine") || normalized.contains("drivetrain")) {
            return "engine-drivetrain";
        }
        if (normalized.contains("wheel")) {
            return "wheels";
        }
        if (normalized.contains("other")) {
            return "others";
        }
        if (normalized.contains("off-road") || normalized.contains("utility")) {
            return "off-road-utility";
        }
        return "uncategorized";
    }

    private String normalizeLookupToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("[-_]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractMileageFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        Matcher hintedMatcher = MILEAGE_HINT_PATTERN.matcher(rawText);
        while (hintedMatcher.find()) {
            String numberPart = hintedMatcher.group(1);
            String unitPart = hintedMatcher.group(2);
            String candidate = numberPart + (unitPart == null ? "" : (" " + unitPart));
            String normalized = normalizeIntegerString(candidate, 0, 3_000_000, true, true);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        Matcher unitMatcher = GENERIC_UNIT_DISTANCE_PATTERN.matcher(rawText);
        while (unitMatcher.find()) {
            String candidate = unitMatcher.group(1) + " " + unitMatcher.group(2);
            String normalized = normalizeIntegerString(candidate, 0, 3_000_000, true, true);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
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
            return Math.max(240, Math.min(2200, parsed));
        } catch (Exception ignored) {
            return 700;
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
                    "\"servicesHistory\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"upgrades\":{\"value\":\"\",\"confidence\":0.0}," +
                    "\"notesAndGrades\":{\"value\":\"\",\"confidence\":0.0}," +
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
                    "}}. Rules: copy all visible text into rawText verbatim (including bullets and line breaks). " +
                    "Description must keep the full descriptive text verbatim (do not summarize). " +
                    "Use empty strings for unknown values and confidence between 0 and 1. " +
                    "categorySlug must be a lowercase hyphen slug (not a display name). " +
                    "mileage must be numeric in kilometers only (no units, commas, or dots).";
        }

        return "Extract automotive product details from the image and return JSON with this shape: " +
                "{\"rawText\":\"...\",\"fields\":{" +
                "\"name\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"price\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"brand\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"description\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"servicesHistory\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"upgrades\":{\"value\":\"\",\"confidence\":0.0}," +
                "\"notesAndGrades\":{\"value\":\"\",\"confidence\":0.0}," +
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
                "}}. Rules: Use empty strings for unknown values. Confidence must be 0-1. " +
                "Copy all visible text into rawText verbatim and keep visible line breaks. " +
                "Do not summarize description; keep all key details visible in the image. " +
                "For wheels, extract diameter (e.g. 18), width (e.g. 8.5), bolt pattern (e.g. 5x112), offset/ET (e.g. 35). "
                +
                "For engines, extract type (inline-4/v6/v8), displacement in cc, power in hp. " +
                "For cars, extract bodyType (sedan/coupe/suv/etc), driveType (fwd/rwd/awd), transmission type. " +
                "For parts, identify position (front/rear/left/right) and category depth where visible. " +
                "categorySlug must be a lowercase hyphen slug (not a display name). " +
                "mileage must be numeric in kilometers only (no units, commas, or dots).";
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
