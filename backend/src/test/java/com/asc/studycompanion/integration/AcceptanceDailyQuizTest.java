package com.asc.studycompanion.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End API Acceptance Suite covering the Guardian 2-Option Quiz Bank,
 * 10-question sequential rotation gate, anti-IDOR / RBAC guards, validation,
 * and Guardian statistics dashboard.
 */
class AcceptanceDailyQuizTest {

    private static Clock fixedVnClock(int year, int month, int day) {
        Instant instant = LocalDate.of(year, month, day)
                .atTime(8, 0)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant();
        return Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    @DisplayName("Full E2E 30-question sequential rotation (1-10 -> 11-20 -> 21-30 -> 1-10), answer masking & Guardian stats")
    void fullCycleSequentialRotationAndStatsAcrossFourDays() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(fixedVnClock(2026, 11, 23));

        String studentJwt = registerAndGetToken(h, "student.quiz@example.com", "Bé Quyên", "STUDENT", 6);
        String guardianJwt = registerAndGetToken(h, "guardian.quiz@example.com", "Mẹ Quyên", "GUARDIAN", null);
        linkStudentAndGuardian(h, studentJwt, guardianJwt);

        // Bulk-create 30 questions (10 batches of 3 across 30 days simulation)
        List<Map<String, Object>> questionsPayload = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            questionsPayload.add(Map.of(
                    "questionText", "Câu trắc nghiệm số " + i,
                    "optionA", "Lựa chọn A" + i,
                    "optionB", "Lựa chọn B" + i,
                    "correctOption", i % 2 == 1 ? "A" : "B",
                    "explanation", "Giải thích số " + i,
                    "topicLabel", i <= 15 ? "Toán" : "Tiếng Anh",
                    "active", true
            ));
        }

        h.mockMvc.perform(post("/api/guardian/quiz/questions/bulk")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("questions", questionsPayload))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(30));

        // Configure daily quiz: enabled, 10/day, Mon-Sun, sequential rotation
        h.mockMvc.perform(put("/api/guardian/quiz/settings")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "enabled", true,
                                "questionsPerDay", 10,
                                "activeWeekdays", List.of(1, 2, 3, 4, 5, 6, 7),
                                "selectionMode", "SEQUENTIAL_ROTATION",
                                "shuffleOptions", false,
                                "resetCursor", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextBatchLabel").value("Câu 1–10 / 30"));

        // Day 1: 1-10
        h.dailyQuizService.setClock(fixedVnClock(2026, 11, 23));
        completeDayQuiz(h, studentJwt, 1, 10, true);

        // Day 2: 11-20
        h.dailyQuizService.setClock(fixedVnClock(2026, 11, 24));
        completeDayQuiz(h, studentJwt, 11, 20, false);

        // Day 3 (student skipped Nov 25, logs in Nov 26): 21-30
        h.dailyQuizService.setClock(fixedVnClock(2026, 11, 26));
        completeDayQuiz(h, studentJwt, 21, 30, true);

        // Day 4: wraps back to 1-10
        h.dailyQuizService.setClock(fixedVnClock(2026, 11, 27));
        MvcResult day4Res = h.mockMvc.perform(get("/api/student/daily-quiz/today")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateLocked").value(true))
                .andExpect(jsonPath("$.data.session.items.length()").value(10))
                .andExpect(jsonPath("$.data.session.items[0].questionText").value("Câu trắc nghiệm số 1"))
                .andExpect(jsonPath("$.data.session.items[0].correctOption").doesNotExist())
                .andReturn();

        JsonNode day4Session = readData(h, day4Res).get("session");
        assertEquals(0, day4Session.get("cycleStartIndex").asInt());
        assertEquals(10, day4Session.get("cycleEndIndex").asInt());

        // Guardian views stats + drilldown on Day 1 session
        MvcResult statsRes = h.mockMvc.perform(get("/api/guardian/quiz/stats")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCompletedSessions").value(3))
                .andExpect(jsonPath("$.data.totalQuestionsAnswered").value(30))
                .andExpect(jsonPath("$.data.totalCorrectAnswers").value(25))
                .andExpect(jsonPath("$.data.averageAccuracyPercent").value(83))
                .andReturn();

        JsonNode statsData = readData(h, statsRes);
        assertFalse(statsData.get("hardestQuestions").isEmpty());
        long firstSessionId = statsData.get("dailyStats").get(statsData.get("dailyStats").size() - 1)
                .get("sessionId").asLong();

        h.mockMvc.perform(get("/api/guardian/quiz/sessions/" + firstSessionId)
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(10))
                .andExpect(jsonPath("$.data.cycleLabel").value("Câu 1–10"));
    }

    @Test
    @DisplayName("Security, Anti-IDOR, Unauthenticated & Bean Validation guards block invalid/cross-role requests cleanly")
    void securityAntiIdorAndValidationGuards() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(fixedVnClock(2026, 11, 28));

        String student1Jwt = registerAndGetToken(h, "s1@example.com", "Học sinh 1", "STUDENT", 6);
        String student2Jwt = registerAndGetToken(h, "s2@example.com", "Học sinh 2", "STUDENT", 7);
        String guardian1Jwt = registerAndGetToken(h, "g1@example.com", "Phụ huynh 1", "GUARDIAN", null);
        String guardian2Jwt = registerAndGetToken(h, "g2@example.com", "Phụ huynh 2", "GUARDIAN", null);

        Long student1ProfileId = h.profiles.get(0).getId();
        linkStudentAndGuardian(h, student1Jwt, guardian1Jwt);

        // Unauthenticated -> 401 AUTH_001
        h.mockMvc.perform(get("/api/student/daily-quiz/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        h.mockMvc.perform(get("/api/guardian/quiz/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        // Cross-role -> 403 AUTH_004
        h.mockMvc.perform(get("/api/guardian/quiz/settings")
                        .header("Authorization", "Bearer " + student1Jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        h.mockMvc.perform(get("/api/student/daily-quiz/today")
                        .header("Authorization", "Bearer " + guardian1Jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        // Unlinked Guardian -> 403 GRD_001
        h.mockMvc.perform(get("/api/guardian/quiz/stats")
                        .header("Authorization", "Bearer " + guardian2Jwt)
                        .param("studentId", String.valueOf(student1ProfileId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GRD_001"));

        // Null element validation in bulk quiz & timetable -> 400 VAL_001 (never 500!)
        h.mockMvc.perform(post("/api/guardian/quiz/questions/bulk")
                        .header("Authorization", "Bearer " + guardian1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questions\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + student1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        // Create 1 question + enable quiz for student 1 -> student 2 tries submitting to student 1's session -> 403 AUTH_004
        h.mockMvc.perform(post("/api/guardian/quiz/questions")
                        .header("Authorization", "Bearer " + guardian1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "questionText", "1 + 2 = ?",
                                "optionA", "3",
                                "optionB", "4",
                                "correctOption", "A"
                        ))))
                .andExpect(status().isCreated());

        h.mockMvc.perform(put("/api/guardian/quiz/settings")
                        .header("Authorization", "Bearer " + guardian1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "enabled", true,
                                "questionsPerDay", 10,
                                "activeWeekdays", List.of(1, 2, 3, 4, 5, 6, 7),
                                "selectionMode", "SEQUENTIAL_ROTATION",
                                "shuffleOptions", false
                        ))))
                .andExpect(status().isOk());

        MvcResult s1QuizRes = h.mockMvc.perform(get("/api/student/daily-quiz/today")
                        .header("Authorization", "Bearer " + student1Jwt))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode s1Session = readData(h, s1QuizRes).get("session");
        long sessionId = s1Session.get("sessionId").asLong();
        long itemId = s1Session.get("items").get(0).get("id").asLong();

        h.mockMvc.perform(post("/api/student/daily-quiz/sessions/" + sessionId + "/items/" + itemId + "/answer")
                        .header("Authorization", "Bearer " + student2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOption\":\"A\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
    }

    private void completeDayQuiz(
            InMemoryStudyHarness h,
            String studentJwt,
            int expectedFirstNum,
            int expectedLastNum,
            boolean answerAllCorrect
    ) throws Exception {
        MvcResult todayRes = h.mockMvc.perform(get("/api/student/daily-quiz/today")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateLocked").value(true))
                .andReturn();

        JsonNode session = readData(h, todayRes).get("session");
        JsonNode items = session.get("items");
        assertEquals(10, items.size());
        assertEquals("Câu trắc nghiệm số " + expectedFirstNum, items.get(0).get("questionText").asText());
        assertEquals("Câu trắc nghiệm số " + expectedLastNum, items.get(9).get("questionText").asText());

        long sessionId = session.get("sessionId").asLong();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            assertTrue(item.get("correctOption").isNull(), "Unanswered item must mask correctOption");
            int qNum = expectedFirstNum + i;
            String correctOpt = qNum % 2 == 1 ? "A" : "B";
            String picked = answerAllCorrect ? correctOpt : "A";
            boolean expectLocked = i < items.size() - 1;

            h.mockMvc.perform(post("/api/student/daily-quiz/sessions/" + sessionId + "/items/" + item.get("id").asLong() + "/answer")
                            .header("Authorization", "Bearer " + studentJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"selectedOption\":\"" + picked + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.gateLocked").value(expectLocked));
        }

        // Duplicate answer attempt cleanly returns 409 CONFLICT
        h.mockMvc.perform(post("/api/student/daily-quiz/sessions/" + sessionId + "/items/" + items.get(0).get("id").asLong() + "/answer")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOption\":\"A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SYS_409"));
    }

    private String registerAndGetToken(
            InMemoryStudyHarness h,
            String email,
            String fullName,
            String role,
            Integer grade
    ) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("email", email);
        body.put("password", "secret123");
        body.put("fullName", fullName);
        body.put("role", role);
        if (grade != null) {
            body.put("grade", grade);
        }
        MvcResult res = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(h, res).get("accessToken").asText();
    }

    private void linkStudentAndGuardian(
            InMemoryStudyHarness h,
            String studentJwt,
            String guardianJwt
    ) throws Exception {
        MvcResult codeRes = h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isCreated())
                .andReturn();
        String code = readData(h, codeRes).get("linkCode").asText();

        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", code))))
                .andExpect(status().isOk());
    }

    private JsonNode readData(InMemoryStudyHarness h, MvcResult result) throws Exception {
        return h.objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
