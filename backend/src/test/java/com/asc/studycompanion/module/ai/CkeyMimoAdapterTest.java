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
    void parsesValidEvaluationJsonClampsFeedbackUnder1000AndCapturesUsage() {
        String longFeedback = ("Nhận xét tổng quan: Con trình bày ý chính rõ ràng và bám sát kiến thức trọng tâm. "
                + "Cần khắc phục: Con hãy kiểm tra lại ví dụ minh họa và bổ sung thêm trường hợp đặc biệt theo sách giáo khoa nhé. ").repeat(8);
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
                  "usage": { "prompt_tokens": 512, "completion_tokens": 220 }
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
        assertTrue(!result.feedback().isBlank() && result.feedback().length() <= 1000,
                "Expected <= 1000 chars but got " + result.feedback().length());
        assertTrue(result.feedback().endsWith("."));
        assertEquals(512, result.inputTokens());
        assertEquals(220, result.outputTokens());
    }

    @Test
    void preservesConciseFeedbackWithoutMinimumPaddingWhileStillClampingOver1000() {
        String concise = "Nhận xét tổng quan: Con đã nắm được định nghĩa cơ bản.\nCần khắc phục: Bổ sung thêm ví dụ minh họa.";
        String shortJson = """
                {
                  "accuracy": "PARTIALLY_CORRECT",
                  "completeness": "INCOMPLETE",
                  "overallStatus": "NEEDS_IMPROVEMENT",
                  "feedback": "%s",
                  "incorrectPoints": ["Nhầm dấu khi chuyển vế"],
                  "missingPoints": ["Thiếu 2 ví dụ số hữu tỉ âm"],
                  "confidence": "HIGH"
                }
                """.formatted(concise.replace("\n", "\\n"));
        AiEvaluationResult result = adapter.parseAndValidateJson(shortJson, 200, 80, "mimo-v2-flash");
        assertEquals(concise, result.feedback());

        String leakedBracketsJson = """
                {
                  "accuracy": "CORRECT",
                  "completeness": "COMPLETE",
                  "overallStatus": "GOOD",
                  "feedback": "Con đã trả lời đúng ý chính bài đọc.\\nCần khắc phục: []",
                  "incorrectPoints": [],
                  "missingPoints": [],
                  "confidence": "HIGH"
                }
                """;
        AiEvaluationResult cleaned = adapter.parseAndValidateJson(leakedBracketsJson, 200, 80, "mimo-v2-flash");
        assertTrue(cleaned.feedback().startsWith("Nhận xét tổng quan: "));
        assertTrue(!cleaned.feedback().contains("[]"));
        assertTrue(cleaned.feedback().contains("Cần khắc phục: Bài làm chưa ghi nhận lỗi sai kiến thức"));
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

    @Test
    void enforcesCrossFieldPolicyConsistencyAndCapsPointsList() {
        String inconsistentIncorrect = """
                {
                  "accuracy": "INCORRECT",
                  "completeness": "COMPLETE",
                  "overallStatus": "GOOD",
                  "feedback": "Bài làm hiện đang sai khái niệm trọng tâm về hằng đẳng thức nên chưa đạt yêu cầu, con cần xem kỹ lại công thức trong sách giáo khoa Kết nối tri thức trước khi nộp bản chỉnh sửa tiếp theo nhé.",
                  "incorrectPoints": ["Lỗi 1", "Lỗi 2", "Lỗi 2", "Lỗi 3", "Lỗi 4", "Lỗi 5"],
                  "missingPoints": [],
                  "confidence": "HIGH"
                }
                """;
        AiEvaluationResult downgraded = adapter.parseAndValidateJson(inconsistentIncorrect, 120, 90, "mimo-v2-flash");
        assertEquals(OverallEvaluationStatus.INCORRECT, downgraded.overallStatus());
        assertEquals(4, downgraded.incorrectPoints().size());

        String incompleteMarkedGood = """
                {
                  "accuracy": "CORRECT",
                  "completeness": "INCOMPLETE",
                  "overallStatus": "GOOD",
                  "feedback": "Con làm đúng phần định nghĩa nhưng vẫn còn thiếu ví dụ minh họa quan trọng trong bài học, hãy bổ sung thêm để bài chuẩn bị đầy đủ hơn.",
                  "incorrectPoints": [],
                  "missingPoints": ["Thiếu ví dụ minh họa"],
                  "confidence": "MEDIUM"
                }
                """;
        AiEvaluationResult normalized = adapter.parseAndValidateJson(incompleteMarkedGood, 100, 80, "mimo-v2-flash");
        assertEquals(OverallEvaluationStatus.NEEDS_IMPROVEMENT, normalized.overallStatus());

        String unverifiableMarkedGood = """
                {
                  "accuracy": "UNVERIFIABLE",
                  "completeness": "COMPLETE",
                  "overallStatus": "GOOD",
                  "feedback": "Chưa có nguồn đối chiếu xác thực cho chủ đề này nên không thể xác nhận độ chính xác.",
                  "incorrectPoints": [],
                  "missingPoints": [],
                  "confidence": "LOW"
                }
                """;
        AiEvaluationResult insufficient = adapter.parseAndValidateJson(unverifiableMarkedGood, 100, 80, "mimo-v2-flash");
        assertEquals(OverallEvaluationStatus.INSUFFICIENT_EVIDENCE, insufficient.overallStatus());
        assertEquals(CompletenessLevel.UNDETERMINED, insufficient.completeness());
    }

    @Test
    void clampsBoundarySpaceAt999StrictlyUnder1000AndRejectsJsonNullFeedback() {
        // Space at index 999 must still produce <= 1000 chars after appending "..."
        String boundaryFeedback = "a".repeat(999) + " wordoverflow";
        String clamped = CkeyMimoAdapter.clampFeedbackGracefully(boundaryFeedback);
        assertTrue(clamped.length() <= 1000, "Expected <= 1000 chars, got " + clamped.length());
        assertTrue(clamped.endsWith("..."));

        String nullFeedbackJson = """
                {
                  "accuracy": "CORRECT",
                  "completeness": "COMPLETE",
                  "overallStatus": "GOOD",
                  "feedback": null,
                  "incorrectPoints": [null],
                  "missingPoints": [],
                  "confidence": "HIGH"
                }
                """;
        AppException nullEx = assertThrows(
                AppException.class,
                () -> adapter.parseAndValidateJson(nullFeedbackJson, 10, 10, "mimo-v2-flash")
        );
        assertEquals(ErrorCode.AI_RESPONSE_INVALID, nullEx.getErrorCode());
    }
}
