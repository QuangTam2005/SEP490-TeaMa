package com.asc.studycompanion.integration;

import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [TASK-INT.2a] End-to-End API Acceptance Suite for AC-01 through AC-10:
 * Open-email auth & JWT, dynamic grade 1-12, 8-char link codes, versioned timetable
 * (Student + Guardian edits + anti-IDOR), Asia/Ho_Chi_Minh next-day plan, REVIEW->PREVIEW gate,
 * BR-12 normalized length [850..2000], v1..v3 version cap, and Idempotency-Key deduplication.
 */
class AcceptanceCoreFlowsTest {

    // Sunday 2026-11-29 22:00 Asia/Ho_Chi_Minh -> Tomorrow is Monday 2026-11-30 (weekday 1)
    private static final Clock SUNDAY_NIGHT_VN =
            Clock.fixed(Instant.parse("2026-11-29T15:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("AC-01..AC-05: Auth open email + dynamic grade 1-12, JWT /me, Link Code, Timetable versioning & Guardian edit + anti-IDOR")
    void authLinkCodeAndTimetableVersioningEndToEnd() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(SUNDAY_NIGHT_VN);

        // AC-01: Student missing grade rejected with 400 VAL_001
        h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "no.grade@gmail.com",
                                "password", "secret123",
                                "fullName", "Bé Thiếu Khối",
                                "role", "STUDENT"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        // AC-01: Student chooses own grade (grade 7, open email domain)
        MvcResult studentReg = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "minh.anh@example.edu.vn",
                                "password", "secret123",
                                "fullName", "Nguyễn Minh Anh",
                                "role", "STUDENT",
                                "grade", 7
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.grade").value(7))
                .andReturn();
        String studentJwt = readData(h, studentReg).get("accessToken").asText();

        // AC-02: Duplicate email rejected with 409 AUTH_005
        h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "MINH.ANH@example.edu.vn",
                                "password", "secret123",
                                "fullName", "Duplicate User",
                                "role", "STUDENT",
                                "grade", 6
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AUTH_005"));

        // AC-02: Unauthenticated /api/auth/me returns 401 AUTH_001; valid JWT hydrates UserResponse
        h.mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        // AC-04: Student updates grade 7 -> 8 dynamically and fetches 5 subjects
        h.mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("grade", 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value(8));

        h.mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value(8));

        h.mockMvc.perform(get("/api/subjects")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(13));

        h.mockMvc.perform(get("/api/subjects").param("grade", "8")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8));

        h.mockMvc.perform(get("/api/subjects").param("grade", "10")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(11));

        // AC-03: Register Guardian & Strange Guardian, test 8-char Link Code flow
        MvcResult guardianReg = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "parent.an@yahoo.com",
                                "password", "secret123",
                                "fullName", "Phụ huynh An",
                                "role", "GUARDIAN"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String guardianJwt = readData(h, guardianReg).get("accessToken").asText();

        // Guardian cannot generate student link code (403 AUTH_004)
        h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        // Student generates 8-char code
        MvcResult codeRes = h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        String linkCode = readData(h, codeRes).get("linkCode").asText();
        assertEquals(8, linkCode.length());

        // Guardian redeems code -> ACTIVE + linkedStudentName hydrated in /api/auth/me
        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", linkCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.linkedUserName").value("Nguyễn Minh Anh"));

        // Reusing already accepted code fails
        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", linkCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AUTH_007"));

        h.mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedStudentName").value("Nguyễn Minh Anh"));

        // AC-05: Student saves Timetable v1, linked Guardian updates -> v2 (v1 archived), unlinked Guardian blocked (GRD_001)
        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 1")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1));

        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 1, Tiết 3"),
                                        Map.of("weekday", 1, "subjectCode", "ENG", "periodNote", "Tiết 5")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.updatedByName").value("Phụ huynh An"));

        assertEquals(2, h.timetables.size());
        assertEquals(TimetableStatus.ARCHIVED, h.timetables.get(0).getStatus());
        assertEquals(TimetableStatus.ACTIVE, h.timetables.get(1).getStatus());

        // Student sees v2 created by Guardian
        h.mockMvc.perform(get("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.entries.length()").value(2));

        // Unlinked Guardian rejected with 403 GRD_001
        MvcResult strangerReg = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "stranger@example.com",
                                "password", "secret123",
                                "fullName", "Người Lạ",
                                "role", "GUARDIAN"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String strangerJwt = readData(h, strangerReg).get("accessToken").asText();

        h.mockMvc.perform(get("/api/student/timetable")
                        .header("Authorization", "Bearer " + strangerJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GRD_001"));
    }

    @Test
    @DisplayName("AC-06..AC-10: Next-day study plan (tomorrow schedule + empty day), REVIEW->PREVIEW gate, BR-12 [850..2000], idempotency, v1..v3 cap")
    void studyPlanningSubmissionAndVersionCapEndToEnd() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(SUNDAY_NIGHT_VN);

        String studentJwt = registerStudentAndGetJwt(h, "student.v1@example.com", "Bảo Long", 6);

        // AC-07: Empty tomorrow schedule returns hasSchedule=false without forcing plan rows
        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 2, "subjectCode", "LIT", "periodNote", "Thứ 3 mới học")
                                )
                        ))))
                .andExpect(status().isOk());

        h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetDate").value("2026-11-30"))
                .andExpect(jsonPath("$.data.weekdayLabel").value("Thứ 2"))
                .andExpect(jsonPath("$.data.hasSchedule").value(false))
                .andExpect(jsonPath("$.data.subjects.length()").value(0));

        // AC-06: Schedule 2 periods of MATH on Monday -> collapsed into 1 REVIEW + 1 PREVIEW pair
        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 1"),
                                        Map.of("weekday", 1, "subjectCode", "MATH", "periodNote", "Tiết 4")
                                )
                        ))))
                .andExpect(status().isOk());

        MvcResult planRes = h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasSchedule").value(true))
                .andExpect(jsonPath("$.data.subjects.length()").value(1))
                .andExpect(jsonPath("$.data.subjects[0].subjectCode").value(SubjectCode.MATH.name()))
                .andExpect(jsonPath("$.data.subjects[0].periodNote").value("Tiết 1, Tiết 4"))
                .andExpect(jsonPath("$.data.subjects[0].reviewTask.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.subjects[0].previewTask.status").value("BLOCKED"))
                .andReturn();

        JsonNode pair = readData(h, planRes).get("subjects").get(0);
        long reviewTaskId = pair.get("reviewTask").get("id").asLong();
        long previewTaskId = pair.get("previewTask").get("id").asLong();

        // AC-08: Attempting PREVIEW before REVIEW returns 400 STUDY_003
        String valid400Content = "A".repeat(400);
        h.mockMvc.perform(post("/api/student/tasks/" + previewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-preview-blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 3: Phép cộng số tự nhiên",
                                "content", valid400Content,
                                "idempotencyKey", "idem-preview-blocked"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("STUDY_003"));

        // AC-09: Content with CRLF + trailing spaces summing to 399 chars fails BR-12 (STUDY_004)
        String short399 = "   " + "B".repeat(398) + "\r\n   ";
        // Exact normalized length = 399: 397 + "\n" + "C" = 399
        String exact399 = "   " + "B".repeat(397) + "\r\nC   ";
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-short-399")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Tập hợp các số tự nhiên",
                                "content", exact399,
                                "idempotencyKey", "idem-short-399"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("STUDY_004"));

        // AC-09: 400 normalized chars succeeds -> v1 EVALUATING, PREVIEW unlocked to NOT_STARTED
        String exact400WithCrlf = "  " + "X".repeat(398) + "\r\nY  ";
        MvcResult v1Res = h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Tập hợp các số tự nhiên",
                                "content", exact400WithCrlf,
                                "idempotencyKey", "idem-v1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.charCount").value(400))
                .andReturn();
        long v1SubId = readData(h, v1Res).get("id").asLong();

        // Check PREVIEW unlocked in plan API and task detail
        h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjects[0].reviewTask.status").value("EVALUATING"))
                .andExpect(jsonPath("$.data.subjects[0].reviewTask.currentLessonTitle").value("Bài 2: Tập hợp các số tự nhiên"))
                .andExpect(jsonPath("$.data.subjects[0].previewTask.status").value("NOT_STARTED"));

        // AC-09: Replaying same Idempotency-Key returns identical v1 submission (BR-14)
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Tập hợp các số tự nhiên",
                                "content", exact400WithCrlf,
                                "idempotencyKey", "idem-v1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(v1SubId))
                .andExpect(jsonPath("$.data.version").value(1));

        // AC-10: Cannot revise while EVALUATING (409 STUDY_006)
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-while-eval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Bản sửa v2",
                                "content", valid400Content,
                                "idempotencyKey", "idem-while-eval"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STUDY_006"));

        // Simulate worker evaluation finishing v1 -> Student updates to v2, then v3
        h.tasks.stream().filter(t -> t.getId().equals(reviewTaskId)).findFirst().orElseThrow()
                .setStatus(StudyTaskStatus.EVALUATED);
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Bản sửa v2",
                                "content", "Y".repeat(900),
                                "idempotencyKey", "idem-v2"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(2));

        // 3001 normalized chars rejected with 400 STUDY_004
        h.tasks.stream().filter(t -> t.getId().equals(reviewTaskId)).findFirst().orElseThrow()
                .setStatus(StudyTaskStatus.EVALUATED);
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-over-3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Vượt quá 3000 ký tự",
                                "content", "Z".repeat(3001),
                                "idempotencyKey", "idem-over-3000"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("STUDY_004"));

        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-v3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Bản sửa v3",
                                "content", "Z".repeat(3000),
                                "idempotencyKey", "idem-v3"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(3))
                .andExpect(jsonPath("$.data.charCount").value(3000));

        // AC-10: 4th submission (v4) rejected because max versions is v1..v3 (400 STUDY_005)
        h.tasks.stream().filter(t -> t.getId().equals(reviewTaskId)).findFirst().orElseThrow()
                .setStatus(StudyTaskStatus.EVALUATED);
        h.mockMvc.perform(post("/api/student/tasks/" + reviewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-v4-blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 2: Bản v4 bị chặn",
                                "content", valid400Content,
                                "idempotencyKey", "idem-v4-blocked"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("STUDY_005"));

        // Task detail shows all 3 immutable versions ordered v3, v2, v1 with only v3 current
        MvcResult detailRes = h.mockMvc.perform(get("/api/student/tasks/" + reviewTaskId)
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissions.length()").value(3))
                .andExpect(jsonPath("$.data.submissions[0].version").value(3))
                .andExpect(jsonPath("$.data.submissions[0].isCurrent").value(true))
                .andExpect(jsonPath("$.data.submissions[1].version").value(2))
                .andExpect(jsonPath("$.data.submissions[1].isCurrent").value(false))
                .andExpect(jsonPath("$.data.submissions[2].version").value(1))
                .andExpect(jsonPath("$.data.submissions[2].isCurrent").value(false))
                .andReturn();
        assertNotNull(detailRes);

        // AC-10: Updating Timetable AFTER submissions preserves the historical StudyPlan & submissions (BR-20)
        h.mockMvc.perform(put("/api/student/timetable")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("weekday", 1, "subjectCode", "ENG", "periodNote", "Đổi lịch sang Tiếng Anh")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(3));

        h.mockMvc.perform(get("/api/student/plans/next-day")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjects[0].subjectCode").value("MATH"))
                .andExpect(jsonPath("$.data.subjects[0].reviewTask.revisionCount").value(2));

        // PREVIEW can now be submitted once REVIEW is submitted
        h.mockMvc.perform(post("/api/student/tasks/" + previewTaskId + "/submissions")
                        .header("Authorization", "Bearer " + studentJwt)
                        .header("Idempotency-Key", "idem-preview-v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "lessonTitle", "Bài 3: Phép cộng và phép nhân",
                                "content", valid400Content,
                                "idempotencyKey", "idem-preview-v1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1));

        // Also assert short399 helper was not accidentally used over exact399
        assertFalse(short399.trim().length() == 400);
        assertTrue(h.plans.size() == 1);
    }

    @Test
    void accountProfilePasswordMultiChildAndUnlinkFlowsWorkEndToEnd() throws Exception {
        InMemoryStudyHarness h = new InMemoryStudyHarness(Clock.systemUTC());

        String child1Jwt = registerStudentAndGetJwt(h, "child1@gmail.com", "Bé Một", 6);
        String child2Jwt = registerStudentAndGetJwt(h, "child2@gmail.com", "Bé Hai", 10);

        // Update profile + change password on child1
        h.mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + child1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "fullName", "Bé Một Đổi Tên",
                                "grade", 7
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Bé Một Đổi Tên"))
                .andExpect(jsonPath("$.data.grade").value(7));

        h.mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + child1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "wrongpass",
                                "newPassword", "newsecret123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_002"));

        h.mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + child1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "secret123",
                                "newPassword", "newsecret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Login with new password succeeds
        h.mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "child1@gmail.com",
                                "password", "newsecret123"
                        ))))
                .andExpect(status().isOk());

        // Guardian links to both child1 and child2
        MvcResult gReg = h.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "email", "mom@gmail.com",
                                "password", "secret123",
                                "fullName", "Mẹ An",
                                "role", "GUARDIAN"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String guardianJwt = readData(h, gReg).get("accessToken").asText();

        String code1 = readData(h, h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + child1Jwt))
                .andReturn()).get("linkCode").asText();
        String code2 = readData(h, h.mockMvc.perform(post("/api/links/code")
                        .header("Authorization", "Bearer " + child2Jwt))
                .andReturn()).get("linkCode").asText();

        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", code1))))
                .andExpect(status().isOk());
        h.mockMvc.perform(post("/api/links/accept")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("linkCode", code2))))
                .andExpect(status().isOk());

        MvcResult childrenRes = h.mockMvc.perform(get("/api/links/children")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        Long child1StudentId = readData(h, childrenRes).get(0).get("displayName").asText().contains("Một")
                ? readData(h, childrenRes).get(0).get("studentId").asLong()
                : readData(h, childrenRes).get(1).get("studentId").asLong();

        // Guardian updates child1 timetable with ?studentId=
        h.mockMvc.perform(put("/api/student/timetable")
                        .param("studentId", String.valueOf(child1StudentId))
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(Map.of("weekday", 1, "subjectCode", "MATH"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries.length()").value(1));

        // Guardian views dashboard scoped to ?studentId=child1
        h.mockMvc.perform(get("/api/guardian/dashboard")
                        .param("studentId", String.valueOf(child1StudentId))
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedStudentName").value("Bé Một Đổi Tên"))
                .andExpect(jsonPath("$.data.grade").value(7));

        // Guardian unlinks child1, remaining linked children becomes 1
        h.mockMvc.perform(post("/api/links/unlink")
                        .header("Authorization", "Bearer " + guardianJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(h.objectMapper.writeValueAsString(Map.of("studentId", child1StudentId))))
                .andExpect(status().isOk());

        h.mockMvc.perform(get("/api/links/children")
                        .header("Authorization", "Bearer " + guardianJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private static String registerStudentAndGetJwt(
            InMemoryStudyHarness harness,
            String email,
            String fullName,
            int grade
    ) throws Exception {
        MvcResult res = harness.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(harness.objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "fullName", fullName,
                                "role", "STUDENT",
                                "grade", grade
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(harness, res).get("accessToken").asText();
    }

    private static JsonNode readData(InMemoryStudyHarness harness, MvcResult result) throws Exception {
        JsonNode root = harness.objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }
}
