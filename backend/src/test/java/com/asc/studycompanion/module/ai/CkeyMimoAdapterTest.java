package com.asc.studycompanion.module.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.provider.AiEvaluationInput;
import com.asc.studycompanion.module.ai.provider.AiEvaluationResult;
import com.asc.studycompanion.module.ai.provider.CkeyMimoAdapter;
import com.asc.studycompanion.module.ai.provider.EvaluationPromptBuilder;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CkeyMimoAdapterTest {

    private MockRestServiceServer server;
    private CkeyMimoAdapter adapter;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        props.getCkey().setBaseUrl("https://api.ckey.example/v1");
        props.getCkey().setApiKey("test-ckey-secret");
        props.getCkey().setModel("mimo-v2-flash");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CkeyMimoAdapter(
                builder.build(),
                props,
                new EvaluationPromptBuilder(props),
                new ObjectMapper()
        );
    }

    @Test
    void parsesValidEvaluationJsonClampsFeedbackUnder600AndCapturesUsage() {
        String longFeedback = "Câu thứ nhất nhận xét rất rõ ràng và tích cực cho bài làm của con. ".repeat(12);
        String innerJson = """
                {
                  "accuracy": "CORRECT",
                  "completeness": "INCOMPLETE",
                  "overallStatus": "NEEDS_IMPROVEMENT",
                  "feedback": "%s",
                  "incorrectPoints": [],
                  "missingPoints": ["Thiếu 1 ví dụ minh họa"],
                  "confidence": "HIGH"
                }
                """.formatted(longFeedback).replace("\n", "");

        String escaped = innerJson.replace("\"", "\\\"");
        String chatResponse = """
                {
                  "choices": [
                    { "message": { "content": "```json\\n%s\\n```" } }
                  ],
                  "usage": { "prompt_tokens": 512, "completion_tokens": 144 }
                }
                """.formatted(escaped);

        server.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ckey-secret"))
                .andRespond(withSuccess(chatResponse, MediaType.APPLICATION_JSON));

        AiEvaluationInput input = new AiEvaluationInput(
                7,
                SubjectCode.MATH,
                "Toán",
                StudyTaskType.PREVIEW,
                "Số hữu tỉ",
                "a".repeat(900),
                List.of()
        );

        AiEvaluationResult result = adapter.evaluate(input);
        server.verify();

        assertEquals(AccuracyLevel.CORRECT, result.accuracy());
        assertEquals(CompletenessLevel.INCOMPLETE, result.completeness());
        assertEquals(OverallEvaluationStatus.NEEDS_IMPROVEMENT, result.overallStatus());
        assertEquals(ConfidenceLevel.HIGH, result.confidence());
        assertEquals(1, result.missingPoints().size());
        assertTrue(result.feedback().length() >= 300 && result.feedback().length() <= 600);
        assertTrue(result.feedback().endsWith("."));
        assertEquals(512, result.inputTokens());
        assertEquals(144, result.outputTokens());
    }

    @Test
    void padsShortFeedbackToAtLeast300CharsWithin600Bound() {
        String shortJson = """
                {
                  "accuracy": "PARTIALLY_CORRECT",
                  "completeness": "INCOMPLETE",
                  "overallStatus": "NEEDS_IMPROVEMENT",
                  "feedback": "Con đã nắm được định nghĩa cơ bản nhưng cần bổ sung ví dụ minh họa rõ hơn.",
                  "incorrectPoints": ["Nhầm dấu khi chuyển vế"],
                  "missingPoints": ["Thiếu 2 ví dụ số hữu tỉ âm"],
                  "confidence": "HIGH"
                }
                """;
        AiEvaluationResult result = adapter.parseAndValidateJson(shortJson, 200, 80, "laguna-xs-2.1");
        assertTrue(result.feedback().length() >= 300,
                "Expected >= 300 chars but got " + result.feedback().length());
        assertTrue(result.feedback().length() <= 600);
    }

    @Test
    void rejectsMalformedJsonAndMapsHttpErrorsToUnavailable() {
        server.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AiEvaluationInput input = new AiEvaluationInput(
                6,
                SubjectCode.LIT,
                "Ngữ văn",
                StudyTaskType.REVIEW,
                "Thánh Gióng",
                "a".repeat(900),
                List.of()
        );

        AppException unavailable = assertThrows(AppException.class, () -> adapter.evaluate(input));
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, unavailable.getErrorCode());

        AppException invalid = assertThrows(
                AppException.class,
                () -> adapter.parseAndValidateJson("{\"accuracy\":\"UNKNOWN_ENUM\"}", 10, 10, "mimo")
        );
        assertEquals(ErrorCode.AI_RESPONSE_INVALID, invalid.getErrorCode());
    }
}
