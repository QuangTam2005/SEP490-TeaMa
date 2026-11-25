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
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible Chat Completions adapter calling MiMo v2.6 Flash via CKEY.
 */
@Slf4j
@Service
public class CkeyMimoAdapter implements AiEvaluationProvider {

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
                "max_tokens", 700,
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
            if (feedbackNode == null || feedbackNode.asText().isBlank()) {
                throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "AI feedback missing");
            }
            String feedback = clampFeedbackGracefully(feedbackNode.asText().trim());

            List<String> incorrectPoints = readStringList(root.get("incorrectPoints"));
            List<String> missingPoints = readStringList(root.get("missingPoints"));

            String canonicalJson = objectMapper.writeValueAsString(root);
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
     * Enforces <= 600 chars without abruptly cutting mid-word or mid-sentence when possible.
     */
    public static String clampFeedbackGracefully(String rawFeedback) {
        if (rawFeedback.length() <= StudyTextUtils.MAX_FEEDBACK_CHARS) {
            return rawFeedback;
        }
        String slice = rawFeedback.substring(0, StudyTextUtils.MAX_FEEDBACK_CHARS);
        int lastSentence = Math.max(slice.lastIndexOf('.'), Math.max(slice.lastIndexOf('!'), slice.lastIndexOf('?')));
        if (lastSentence >= 250) {
            return slice.substring(0, lastSentence + 1).trim();
        }
        int lastSpace = slice.lastIndexOf(' ');
        if (lastSpace > 400) {
            return slice.substring(0, lastSpace).trim() + "...";
        }
        return slice.substring(0, StudyTextUtils.MAX_FEEDBACK_CHARS - 3).trim() + "...";
    }

    private static <E extends Enum<E>> E parseEnum(JsonNode root, String field, Class<E> enumType) {
        JsonNode node = root.get(field);
        if (node == null || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing enum field: " + field);
        }
        return Enum.valueOf(enumType, node.asText().trim());
    }

    private static List<String> readStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item != null && !item.asText().isBlank()) {
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
