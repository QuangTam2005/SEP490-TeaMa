package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.study.util.StudyTextUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI-compatible Chat Completions adapter calling MiMo v2.6 Flash via CKEY.
 */
@Slf4j
@Service
public class CkeyMimoAdapter implements AiEvaluationProvider {

    private static final int MAX_POINT_ITEMS = 4;
    private static final int MAX_POINT_CHARS = 160;
    private static final int LLM_MAX_OUTPUT_TOKENS = 1045;

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final EvaluationPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public CkeyMimoAdapter(
            @Qualifier("ckeyRestClient") RestClient restClient,
            AiProperties aiProperties,
            EvaluationPromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.aiProperties = aiProperties;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiEvaluationResult evaluate(AiEvaluationInput input) {
        String baseUrl = aiProperties.getCkey().getBaseUrl();
        String apiKey = aiProperties.getCkey().getApiKey();
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "CKEY AI provider is not configured");
        }

        String model = aiProperties.getCkey().getModel();
        String endpoint = normalizeChatCompletionsUrl(baseUrl);

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", LLM_MAX_OUTPUT_TOKENS,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", promptBuilder.buildSystemPrompt()),
                        Map.of("role", "user", "content", promptBuilder.buildUserPrompt(input))
                )
        );

        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .body(payload)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientException ex) {
            log.warn("CKEY AI request failed: {}", ex.getMessage());
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Không thể kết nối dịch vụ AI");
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
        }

        String rawContent = stripMarkdownFences(response.choices().get(0).message().content());
        int inputTokens = response.usage() != null && response.usage().promptTokens() != null
                ? response.usage().promptTokens()
                : 0;
        int outputTokens = response.usage() != null && response.usage().completionTokens() != null
                ? response.usage().completionTokens()
                : 0;

        return parseAndValidateJson(rawContent, inputTokens, outputTokens, model);
    }

    public AiEvaluationResult parseAndValidateJson(
            String rawJson,
            int inputTokens,
            int outputTokens,
            String modelUsed
    ) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            AccuracyLevel accuracy = parseEnum(root, "accuracy", AccuracyLevel.class);
            CompletenessLevel completeness = parseEnum(root, "completeness", CompletenessLevel.class);
            OverallEvaluationStatus overallStatus = parseEnum(root, "overallStatus", OverallEvaluationStatus.class);
            ConfidenceLevel confidence = parseEnum(root, "confidence", ConfidenceLevel.class);

            JsonNode feedbackNode = root.get("feedback");
            if (feedbackNode == null || feedbackNode.isNull() || !feedbackNode.isTextual() || feedbackNode.asText().isBlank()) {
                throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "AI feedback missing");
            }

            List<String> incorrectPoints = normalizePointList(readStringList(root.get("incorrectPoints")));
            List<String> missingPoints = normalizePointList(readStringList(root.get("missingPoints")));

            // Enforce cross-field evaluation policy consistency
            if (overallStatus == OverallEvaluationStatus.INSUFFICIENT_EVIDENCE
                    || accuracy == AccuracyLevel.UNVERIFIABLE) {
                overallStatus = OverallEvaluationStatus.INSUFFICIENT_EVIDENCE;
                accuracy = AccuracyLevel.UNVERIFIABLE;
                completeness = CompletenessLevel.UNDETERMINED;
            } else if (accuracy == AccuracyLevel.INCORRECT) {
                if (overallStatus == OverallEvaluationStatus.GOOD
                        || overallStatus == OverallEvaluationStatus.NEEDS_IMPROVEMENT) {
                    overallStatus = OverallEvaluationStatus.INCORRECT;
                }
            } else if (overallStatus == OverallEvaluationStatus.GOOD
                    && (completeness == CompletenessLevel.INCOMPLETE
                    || accuracy == AccuracyLevel.PARTIALLY_CORRECT)) {
                overallStatus = OverallEvaluationStatus.NEEDS_IMPROVEMENT;
            }

            String feedback = ensureFeedbackBounds(feedbackNode.asText().trim(), incorrectPoints, missingPoints);

            Map<String, Object> canonicalMap = new LinkedHashMap<>();
            canonicalMap.put("accuracy", accuracy.name());
            canonicalMap.put("completeness", completeness.name());
            canonicalMap.put("overallStatus", overallStatus.name());
            canonicalMap.put("feedback", feedback);
            canonicalMap.put("incorrectPoints", incorrectPoints);
            canonicalMap.put("missingPoints", missingPoints);
            canonicalMap.put("confidence", confidence.name());
            String canonicalJson = objectMapper.writeValueAsString(canonicalMap);
            return new AiEvaluationResult(
                    accuracy,
                    completeness,
                    overallStatus,
                    feedback,
                    incorrectPoints,
                    missingPoints,
                    confidence,
                    inputTokens,
                    outputTokens,
                    modelUsed,
                    canonicalJson
            );
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("Malformed AI evaluation JSON: {}", ex.getMessage());
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    /**
     * Enforces maximum 1,000-character bound on AI feedback without imposing any artificial minimum length padding.
     */
    public static String ensureFeedbackBounds(
            String rawFeedback,
            List<String> incorrectPoints,
            List<String> missingPoints
    ) {
        return clampFeedbackGracefully(rawFeedback);
    }

    /**
     * Enforces <= 1,000 chars without abruptly cutting mid-word or mid-sentence when input exceeds 1,000 chars.
     */
    public static String clampFeedbackGracefully(String rawFeedback) {
        if (rawFeedback == null) {
            return "";
        }
        String trimmed = rawFeedback.trim();
        if (trimmed.length() <= StudyTextUtils.MAX_FEEDBACK_CHARS) {
            return trimmed;
        }
        String slice = trimmed.substring(0, StudyTextUtils.MAX_FEEDBACK_CHARS);
        int lastSentence = Math.max(slice.lastIndexOf('.'), Math.max(slice.lastIndexOf('!'), slice.lastIndexOf('?')));
        if (lastSentence >= 500) {
            return slice.substring(0, lastSentence + 1).trim();
        }
        String ellipsisWindow = trimmed.substring(0, StudyTextUtils.MAX_FEEDBACK_CHARS - 3);
        int lastSpace = ellipsisWindow.lastIndexOf(' ');
        if (lastSpace >= 550) {
            return ellipsisWindow.substring(0, lastSpace).trim() + "...";
        }
        return ellipsisWindow.trim() + "...";
    }

    private static <E extends Enum<E>> E parseEnum(JsonNode root, String field, Class<E> enumType) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing enum field: " + field);
        }
        return Enum.valueOf(enumType, node.asText().trim());
    }

    private static List<String> normalizePointList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        List<String> seenLower = new ArrayList<>();
        for (String item : raw) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String cleaned = item.replaceAll("\\s+", " ").trim();
            if (cleaned.length() > MAX_POINT_CHARS) {
                int spaceIdx = cleaned.lastIndexOf(' ', MAX_POINT_CHARS - 3);
                cleaned = (spaceIdx > 60 ? cleaned.substring(0, spaceIdx) : cleaned.substring(0, MAX_POINT_CHARS - 3)).trim() + "...";
            }
            String key = cleaned.toLowerCase(Locale.ROOT);
            if (!seenLower.contains(key)) {
                seenLower.add(key);
                normalized.add(cleaned);
                if (normalized.size() >= MAX_POINT_ITEMS) {
                    break;
                }
            }
        }
        return normalized;
    }

    private static List<String> readStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item != null && !item.isNull() && item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        }
        return result;
    }

    private static String stripMarkdownFences(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private static String normalizeChatCompletionsUrl(String baseUrl) {
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        return trimmed.endsWith("/chat/completions") ? trimmed : trimmed + "/chat/completions";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(
            @JsonProperty("choices") List<Choice> choices,
            @JsonProperty("usage") Usage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(@JsonProperty("message") Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("content") String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens
    ) {
    }
}
