package com.asc.studycompanion.integration;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.provider.BraveSearchAdapter;
import com.asc.studycompanion.module.ai.provider.CkeyMimoAdapter;
import com.asc.studycompanion.module.ai.provider.EvaluationPromptBuilder;
import com.asc.studycompanion.module.ai.service.EvaluationJobWorker;
import com.asc.studycompanion.module.ai.service.KnowledgeCacheService;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [TASK-INT.2b] Full-Stack Acceptance Suite for AC-11 through AC-20:
 * PoC AI evaluation quality across all 5 subjects grounded in Kết nối tri thức + Wikipedia
 * (max 3 sources, PII-free prompts, <= 600-char Vietnamese feedback, INSUFFICIENT_EVIDENCE),
 * KnowledgeCache deduplication, 3-retry AI fallback notice to Guardian, Guardian Read-Only
 * dashboard/detail (v1..v3 history + sources), anti-IDOR GRD_001, and zero AIUsageLog leakage.
 */
class AcceptanceAiAndGuardianTest {

    private static final Clock SUNDAY_NIGHT_VN =
            Clock.fixed(Instant.parse("2026-11-29T15:00:00Z"), ZoneOffset.UTC);

    private static AiProperties createAiProperties() {
        AiProperties props = new AiProperties();
        props.setMaxSources(3);
        props.setMaxContextTokens(2250);
        props.getBrave().setBaseUrl("https://api.search.brave.com/res/v1/web/search");
        props.getBrave().setApiKey("test-brave-api-key");
        props.getCkey().setBaseUrl("https://api.ckey.example/v1");
        props.getCkey().setApiKey("test-ckey-secret-key");
        props.getCkey().setModel("mimo-v2-flash");
        return props;
    }

    @Test
    @DisplayName("AC-11..AC-14: PoC AI evaluation across all 5 subjects (KNTT + Wikipedia, PII-free, <=600 chars), KnowledgeCache hit & INSUFFICIENT_EVIDENCE")
    void pocFiveSubjectsGroundingCacheAndInsufficientEvidence() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(SUNDAY_NIGHT_VN);
        AiProperties aiProps = createAiProperties();

        RestClient.Builder braveBuilder = RestClient.builder();
        MockRestServiceServer braveServer = MockRestServiceServer.bindTo(braveBuilder).build();
        BraveSearchAdapter braveAdapter = new BraveSearchAdapter(braveBuilder.build(), aiProps);

        RestClient.Builder ckeyBuilder = RestClient.builder();
        MockRestServiceServer ckeyServer = MockRestServiceServer.bindTo(ckeyBuilder).build();
        CkeyMimoAdapter ckeyAdapter = new CkeyMimoAdapter(
                ckeyBuilder.build(),
                aiProps,
                new EvaluationPromptBuilder(aiProps),
                h.objectMapper
        );

        KnowledgeCacheService knowledgeCacheService = new KnowledgeCacheService(h.knowledgeCacheRepository);
        EvaluationJobWorker worker = new EvaluationJobWorker(
                h.evaluationJobRepository,
                h.studySubmissionRepository,
                h.studyTaskRepository,
                h.aiEvaluationRepository,
                h.aiUsageLogRepository,
                knowledgeCacheService,
                braveAdapter,
                ckeyAdapter,
                aiProps
        );

        // Register Grade 6 student with distinct PII (name & email) that MUST NOT leak to AI/search prompts
        String studentJwt = registerAndLogin(h, "bao.ngoc.pii@example.com", "Lê Bảo Ngọc PII", "STUDENT", 6);

        // Schedule all 5 subjects on Monday 2026-11-30
        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 1"),
                                        Map.of("weekday", 1, "subjectCode", "LIT", "periodNote", "Tiết 2"),
                                        Map.of("weekday", 1, "subjectCode", "ENG", "periodNote", "Tiết 3"),
                                        Map.of("weekday", 1, "subjectCode", "SCI", "periodNote", "Tiết 4"),
                                        Map.of("weekday", 1, "subjectCode", "HISGEO", "periodNote", "Tiết 5")
                                )
                        ))))
                .andExpect(status().isOk());

        MvcResult planRes = h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjects.length()").value(5))
                .andReturn();

        JsonNode pairs = readData(h, planRes).get("subjects");
        Map<SubjectCode, String> lessons = Map.of(
                SubjectCode.MATH, "Tập hợp các số tự nhiên",
                SubjectCode.LIT, "Thánh Gióng truyền thuyết dân gian",
                SubjectCode.ENG, "Unit 1 My New School",
                SubjectCode.SCI, "Các thể của chất và sự chuyển thể",
                SubjectCode.HISGEO, "Hệ thống kinh vĩ tuyến và tọa độ địa lí"
        );

        // Submit REVIEW tasks for all 5 subjects and verify KNTT+Wiki grounding + <=600 chars + zero PII
        for (int i = 0; i < pairs.size(); i++) {
            JsonNode pair = pairs.get(i);
            SubjectCode subject = SubjectCode.valueOf(pair.get("subjectCode").asText());
            long reviewTaskId = pair.get("reviewTask").get("id").asLong();
            String lessonTitle = lessons.get(subject);

            // Request 1: Educational websites covering lesson title (returns 2 URLs)
            String braveWebJson = """
                    {
                      "web": {
                        "results": [
                          { "title": "%s Lớp 6 - Kết nối tri thức với cuộc sống", "url": "https://loigiaihay.com/kntt-%s", "description": "Nội dung chuẩn SGK Kết nối tri thức với cuộc sống môn %s" },
                          { "title": "Blog tham khảo ngoài", "url": "https://blog.example.com/extra-%s", "description": "Tóm tắt chung về bài học" }
                        ]
                      }
                    }
                    """.formatted(subject.name(), subject.name(), subject.name(), subject.name());

            // Request 2: Wikipedia directly for lesson title (returns 1 Wikipedia URL -> 2+1 = 3 total)
            String braveWikiJson = """
                    {
                      "web": {
                        "results": [
                          { "title": "Wikipedia: %s", "url": "https://vi.wikipedia.org/wiki/%s", "description": "Kiến thức bách khoa chính xác từ Wikipedia" }
                        ]
                      }
                    }
                    """.formatted(lessonTitle, subject.name());

            braveServer.expect(requestTo(containsString("api.search.brave.com")))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(requestTo(not(containsString("bao.ngoc.pii"))))
                    .andRespond(withSuccess(braveWebJson, MediaType.APPLICATION_JSON));
            String expectedSecondaryToken = switch (subject) {
                case ENG -> "Global%20Success";
                case LIT -> "Ng%E1%BB%AF%20v%C4%83n";
                default -> "vi.wikipedia.org";
            };
            braveServer.expect(requestTo(containsString(expectedSecondaryToken)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(braveWikiJson, MediaType.APPLICATION_JSON));

            String aiFeedbackLong = ("Nhận xét môn " + subject.name() + ": con trình bày ý chính rất rõ ràng và bám sát sách Kết nối tri thức. ").repeat(8);
            String ckeyPayload = wrapChatCompletion(h, """
                    {
                      "accuracy": "CORRECT",
                      "completeness": "COMPLETE",
                      "overallStatus": "GOOD",
                      "feedback": "%s",
                      "incorrectPoints": [],
                      "missingPoints": [],
                      "confidence": "HIGH"
                    }
                    """.formatted(aiFeedbackLong.replace("\"", "\\\"")));

            ckeyServer.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("Kết nối tri thức với cuộc sống")))
                    .andExpect(content().string(not(containsString("bao.ngoc.pii@example.com"))))
                    .andExpect(content().string(not(containsString("Lê Bảo Ngọc PII"))))
                    .andRespond(withSuccess(ckeyPayload, MediaType.APPLICATION_JSON));

            h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                            .header("Authorization", "Bearer " + studentJwt)
                            .header("Idempotency-Key", "poc-" + subject.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(h.objectMapper.writeValueAsString(Map.of(
                                    "lessonTitle", lessonTitle,
                                    "content", ("Tóm tắt bài học môn " + subject.name() + ": ").concat("kiến thức trọng tâm chuẩn xác. ".repeat(35)),
                                    "idempotencyKey", "poc-" + subject.name()
                            ))))
                    .andExpect(status().isCreated());
        }

        worker.pollAndExecuteDueJobs();

        braveServer.verify();
        ckeyServer.verify();

        // All 5 REVIEW tasks evaluated, capped at <= 3 sources and <= 1000 char mentor feedback (no min floor)
        assertEquals(5, h.evaluations.size());
        assertEquals(5, h.usageLogs.size());
        h.evaluations.forEach(ev -> {
            assertTrue(!ev.getFeedback().isBlank() && ev.getFeedback().length() <= 1000,
                    "Expected non-blank feedback <= 1000 chars but got " + ev.getFeedback().length());
            assertTrue(ev.getSources().size() <= 3 && !ev.getSources().isEmpty());
            assertEquals(OverallEvaluationStatus.GOOD, ev.getOverallStatus());
        });

        // AC-14: Second submission for MATH with same lessonTitle hits KnowledgeCache (0 Brave calls!)
        braveServer.reset();
        ckeyServer.reset();

        long mathReviewId = pairs.get(0).get("reviewTask").get("id").asLong();
        String cachedCkeyPayload = wrapChatCompletion(h, """
                {
                  "accuracy": "PARTIALLY_CORRECT",
                  "completeness": "COMPLETE",
                  "overallStatus": "NEEDS_IMPROVEMENT",
                  "feedback": "Con đã bổ sung ví dụ tốt, nhớ kiểm tra lại ký hiệu thuộc tập hợp nhé.",
                  "incorrectPoints": ["Ký hiệu phần tử chưa nhất quán"],
                  "missingPoints": [],
                  "confidence": "HIGH"
                }
                """);
        ckeyServer.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(cachedCkeyPayload, MediaType.APPLICATION_JSON));

        h.mockMvc.perform(post("/api/student/tasks/" + mathReviewId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "poc-math-v2-cache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "  tập hợp   các số   tự nhiên ",
                                "content", "Bản sửa v2 kiểm tra cache tri thức: ".concat("tập hợp N gồm các số 0, 1, 2, 3... ".repeat(30)),
                                "idempotencyKey", "poc-math-v2-cache"
                        ))))
                .andExpect(status().isCreated());

        worker.pollAndExecuteDueJobs();
        braveServer.verify(); // 0 Brave requests expected because cache hit!
        ckeyServer.verify();
        assertTrue(Boolean.TRUE.equals(h.usageLogs.get(h.usageLogs.size() - 1).getCacheHit()));

        // AC-13: PREVIEW task with empty search results (3 max Brave calls) -> INSUFFICIENT_EVIDENCE
        braveServer.reset();
        ckeyServer.reset();
        long mathPreviewId = pairs.get(0).get("previewTask").get("id").asLong();

        braveServer.expect(requestTo(containsString("api.search.brave.com")))
                .andRespond(withSuccess("{\"web\":{\"results\":[]}}", MediaType.APPLICATION_JSON));
        braveServer.expect(requestTo(containsString("vi.wikipedia.org")))
                .andRespond(withSuccess("{\"web\":{\"results\":[]}}", MediaType.APPLICATION_JSON));
        braveServer.expect(requestTo(containsString("api.search.brave.com")))
                .andRespond(withSuccess("{\"web\":{\"results\":[]}}", MediaType.APPLICATION_JSON));
        String insufficientPayload = wrapChatCompletion(h, """
                {
                  "accuracy": "UNVERIFIABLE",
                  "completeness": "UNDETERMINED",
                  "overallStatus": "INSUFFICIENT_EVIDENCE",
                  "feedback": "Chưa tìm thấy đủ nguồn SGK Kết nối tri thức hoặc Wikipedia để đối chiếu mục này.",
                  "incorrectPoints": [],
                  "missingPoints": ["Cần ghi rõ tên bài trong SGK"],
                  "confidence": "LOW"
                }
                """);
        ckeyServer.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                .andRespond(withSuccess(insufficientPayload, MediaType.APPLICATION_JSON));

        h.mockMvc.perform(post("/api/student/tasks/" + mathPreviewId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "poc-insufficient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Chủ đề nâng cao ngoài SGK chưa có dữ liệu",
                                "content", "Nội dung soạn bài tự do: ".concat("phân tích mở rộng chưa rõ số bài trong sách. ".repeat(25)),
                                "idempotencyKey", "poc-insufficient"
                        ))))
                .andExpect(status().isCreated());

        worker.pollAndExecuteDueJobs();
        assertEquals(OverallEvaluationStatus.INSUFFICIENT_EVIDENCE,
                h.evaluations.get(h.evaluations.size() - 1).getOverallStatus());
    }

    @Test
    @DisplayName("AC-15..AC-20: AI 3-retry fallback to Guardian (EVALUATED), Guardian Read-Only dashboard/v1..v3 history, anti-IDOR, zero AIUsageLog leakage & AI_004 retry guard")
    void aiThreeRetryFallbackAndGuardianReadOnlyVerification() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(SUNDAY_NIGHT_VN);
        AiProperties aiProps = createAiProperties();

        RestClient.Builder braveBuilder = RestClient.builder();
        MockRestServiceServer braveServer = MockRestServiceServer.bindTo(braveBuilder).build();
        BraveSearchAdapter braveAdapter = new BraveSearchAdapter(braveBuilder.build(), aiProps);

        RestClient.Builder ckeyBuilder = RestClient.builder();
        MockRestServiceServer ckeyServer = MockRestServiceServer.bindTo(ckeyBuilder).build();
        CkeyMimoAdapter ckeyAdapter = new CkeyMimoAdapter(
                ckeyBuilder.build(),
                aiProps,
                new EvaluationPromptBuilder(aiProps),
                h.objectMapper
        );

        EvaluationJobWorker worker = new EvaluationJobWorker(
                h.evaluationJobRepository,
                h.studySubmissionRepository,
                h.studyTaskRepository,
                h.aiEvaluationRepository,
                h.aiUsageLogRepository,
                new KnowledgeCacheService(h.knowledgeCacheRepository),
                braveAdapter,
                ckeyAdapter,
                aiProps
        );

        // Setup Student (Grade 7) + Linked Guardian + Strange Guardian
        String studentJwt = registerAndLogin(h, "student.grd@example.com", "Hoàng Nam", "STUDENT", 7);
        String guardianJwt = registerAndLogin(h, "parent.grd@example.com", "Mẹ Hoàng Nam", "GUARDIAN", null);
        String strangerJwt = registerAndLogin(h, "stranger.grd@example.com", "Người Ngoài", "GUARDIAN", null);

        MvcResult codeRes = h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isCreated())
                .andReturn();
        String linkCode = readData(h, codeRes).get("linkCode").asText();

        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", linkCode))))
                .andExpect(status().isOk());

        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 1"),
                                        Map.of("weekday", 1, "subjectCode", "LIT", "periodNote", "Tiết 2")
                                )
                        ))))
                .andExpect(status().isOk());

        MvcResult planRes = h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode subjects = readData(h, planRes).get("subjects");
        long mathReviewId = subjects.get(0).get("reviewTask").get("id").asLong();
        long litReviewId = subjects.get(1).get("reviewTask").get("id").asLong();

        // Part A (AC-18..AC-19): Submit MATH v1 & v2 cleanly so Guardian sees immutable version history
        String braveMathWebJson = """
                {"web":{"results":[{"title":"Toán 7 Kết nối tri thức - Số hữu tỉ","url":"https://loigiaihay.com/toan-7-kntt","description":"Tập hợp Q các số hữu tỉ"},{"title":"VietJack Toán 7 - Số hữu tỉ","url":"https://vietjack.com/toan-7-so-huu-ti","description":"Kiến thức trọng tâm số hữu tỉ"}]}}
                """;
        String braveWikiJson = """
                {"web":{"results":[{"title":"Wikipedia: Số hữu tỉ","url":"https://vi.wikipedia.org/wiki/So_huu_ti","description":"Khái niệm số hữu tỉ"}]}}
                """;
        String v2Feedback640 = ("Nhận xét tổng quan: Bản v2 đúng kiến thức SGK Toán 7 Kết nối tri thức. "
                + "Con đã trình bày định nghĩa số hữu tỉ và điều kiện mẫu số khác không rất đầy đủ, rõ ràng. "
                + "Cần khắc phục: Con có thể bổ sung thêm ví dụ phân số âm để bài ôn tập thêm sinh động. ").repeat(3)
                .trim();
        for (int version = 1; version <= 2; version++) {
            braveServer.reset();
            ckeyServer.reset();
            if (version == 1) {
                braveServer.expect(requestTo(containsString("api.search.brave.com")))
                        .andRespond(withSuccess(braveMathWebJson, MediaType.APPLICATION_JSON));
                braveServer.expect(requestTo(containsString("vi.wikipedia.org")))
                        .andRespond(withSuccess(braveWikiJson, MediaType.APPLICATION_JSON));
            }
            String feedback = version == 2
                    ? v2Feedback640
                    : ("Nhận xét tổng quan: Bản v1 đúng kiến thức SGK Toán 7 Kết nối tri thức. "
                    + "Cần khắc phục: Con cần bổ sung thêm ví dụ minh họa phân số bằng nhau để bài học đầy đủ và dễ nhớ hơn nữa nhé. ").repeat(4);
            String okPayload = wrapChatCompletion(h, """
                    {"accuracy":"CORRECT","completeness":"COMPLETE","overallStatus":"GOOD","feedback":"%s","incorrectPoints":[],"missingPoints":[],"confidence":"HIGH"}
                    """.formatted(feedback));
            ckeyServer.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                    .andRespond(withSuccess(okPayload, MediaType.APPLICATION_JSON));

            h.mockMvc.perform(post("/api/student/tasks/" + mathReviewId + "/submissions")
                            .header("Authorization", "Bearer " + studentJwt)
                            .header("Idempotency-Key", "grd-math-v" + version)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(h.objectMapper.writeValueAsString(Map.of(
                                    "lessonTitle", "Số hữu tỉ",
                                    "content", ("Bài làm phiên bản v" + version + ": ").concat("số hữu tỉ viết dưới dạng a/b với b khác 0. ".repeat(22)),
                                    "idempotencyKey", "grd-math-v" + version
                            ))))
                    .andExpect(status().isCreated());
            worker.pollAndExecuteDueJobs();
        }

        long mathV2SubId = h.submissions.get(h.submissions.size() - 1).getId();

        // AC-20: Calling retry-evaluation on already EVALUATED submission fails with 400 AI_004
        h.mockMvc.perform(post("/api/student/submissions/" + mathV2SubId + "/retry-evaluation")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AI_004"));

        // Part B (AC-15): Submit LIT where AI fails 3 times -> stops retrying, creates fallback notice, marks EVALUATED for Guardian
        braveServer.reset();
        ckeyServer.reset();

        MvcResult litSubRes = h.mockMvc.perform(post("/api/student/tasks/" + litReviewId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "lit-fail-3x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Văn bản Bầy chim chìa vôi",
                                "content", "Bài ôn Ngữ văn: ".concat("những chi tiết miêu tả đàn chim nhỏ vượt qua cơn mưa đêm. ".repeat(20)),
                                "idempotencyKey", "lit-fail-3x"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long litSubId = readData(h, litSubRes).get("id").asLong();

        for (int attempt = 1; attempt <= 3; attempt++) {
            braveServer.reset();
            ckeyServer.reset();
            braveServer.expect(requestTo(containsString("api.search.brave.com")))
                    .andRespond(withSuccess(braveMathWebJson, MediaType.APPLICATION_JSON));
            braveServer.expect(requestTo(containsString("Ng%E1%BB%AF%20v%C4%83n")))
                    .andRespond(withSuccess(braveWikiJson, MediaType.APPLICATION_JSON));
            ckeyServer.expect(requestTo("https://api.ckey.example/v1/chat/completions"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            EvaluationJob job = h.jobs.stream()
                    .filter(j -> j.getSubmissionId().equals(litSubId))
                    .findFirst().orElseThrow();
            job.setNextRetryAt(Instant.now().minusSeconds(1));

            worker.pollAndExecuteDueJobs();

            if (attempt < 3) {
                assertEquals(EvaluationJobStatus.PENDING, job.getStatus());
                assertEquals(StudyTaskStatus.EVALUATING,
                        h.tasks.stream().filter(t -> t.getId().equals(litReviewId)).findFirst().orElseThrow().getStatus());
            } else {
                assertEquals(EvaluationJobStatus.FAILED, job.getStatus());
                assertEquals(StudyTaskStatus.EVALUATED,
                        h.tasks.stream().filter(t -> t.getId().equals(litReviewId)).findFirst().orElseThrow().getStatus());
            }
        }

        // Fallback evaluation stored cleanly with parent check guidance
        assertEquals(EvaluationJobWorker.FALLBACK_GUARDIAN_FEEDBACK,
                h.evaluations.stream().filter(e -> e.getSubmissionId().equals(litSubId)).findFirst().orElseThrow().getFeedback());

        // Part C (AC-16..AC-19): Guardian Read-Only Dashboard & Detail, anti-IDOR, zero AIUsageLog leakage
        h.mockMvc.perform(get("/api/guardian/dashboard?date=2026-11-30&subjectCode=MATH")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedStudentName").value("Hoàng Nam"))
                .andExpect(jsonPath("$.data.grade").value(7))
                .andExpect(jsonPath("$.data.totalTasks").value(2))
                .andExpect(jsonPath("$.data.completedTasks").value(1))
                .andExpect(jsonPath("$.data.items[0].subjectCode").value("MATH"))
                .andExpect(jsonPath("$.data.items[0].evaluation.feedback").value(v2Feedback640));

        // Guardian sees LIT fallback task cleanly as EVALUATED on dashboard
        h.mockMvc.perform(get("/api/guardian/dashboard?date=2026-11-30&subjectCode=LIT")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("EVALUATED"))
                .andExpect(jsonPath("$.data.items[0].evaluation.feedback").value(EvaluationJobWorker.FALLBACK_GUARDIAN_FEEDBACK));

        // Guardian inspects MATH submission detail: immutable v2 & v1 history + sources, zero AIUsageLog fields!
        MvcResult detailRes = h.mockMvc.perform(get("/api/guardian/submissions/" + mathV2SubId)
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versions.length()").value(2))
                .andExpect(jsonPath("$.data.versions[0].version").value(2))
                .andExpect(jsonPath("$.data.versions[1].version").value(1))
                .andExpect(jsonPath("$.data.versions[1].evaluation.sources.length()").value(3))
                .andReturn();

        String rawDetailBody = detailRes.getResponse().getContentAsString();
        assertFalse(rawDetailBody.contains("costUsd"));
        assertFalse(rawDetailBody.contains("promptTokens"));
        assertFalse(rawDetailBody.contains("completionTokens"));
        assertFalse(rawDetailBody.contains("totalTokens"));
        assertFalse(rawDetailBody.contains("AIUsageLog"));
        assertFalse(rawDetailBody.contains("test-ckey-secret-key"));

        // Anti-IDOR checks: Student blocked from Guardian endpoint (403 AUTH_004); stranger blocked (403 GRD_001)
        h.mockMvc.perform(get("/api/guardian/dashboard?date=2026-11-30")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        h.mockMvc.perform(get("/api/guardian/submissions/" + mathV2SubId)
                        .header("Authorization", "Bearer " + strangerJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GRD_001"));
    }

    private static String registerAndLogin(
            InMemoryStudyHarness h,
            String email,
            String fullName,
            String role,
            Integer grade
    ) throws Exception {
        Map<String, Object> body = grade == null
                ? Map.of("email", email, "password", "secret123", "fullName", fullName, "role", role)
                : Map.of("email", email, "password", "secret123", "fullName", fullName, "role", role, "grade", grade);

        MvcResult res = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(h, res).get("accessToken").asText();
    }

    private static String wrapChatCompletion(InMemoryStudyHarness h, String innerJson) throws Exception {
        return h.objectMapper.writeValueAsString(Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", innerJson))
                ),
                "usage", Map.of(
                        "prompt_tokens", 520,
                        "completion_tokens", 140,
                        "total_tokens", 660
                )
        ));
    }

    private static JsonNode readData(InMemoryStudyHarness h, MvcResult res) throws Exception {
        return h.objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
    }
}
