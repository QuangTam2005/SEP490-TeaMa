package com.asc.studycompanion.module.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.guardian.controller.GuardianController;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.service.GuardianService;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GuardianControllerTest {

    @Mock
    private GuardianService guardianService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new GuardianController(guardianService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new org.springframework.http.converter.ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void endpointsEnforceAuthReturnDashboardAndDetailWithoutCostTelemetry() throws Exception {
        mockMvc.perform(get("/api/guardian/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal guardian = new UserPrincipal(2L, "g@gmail.com", "h", "Hang", UserRole.GUARDIAN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guardian, null, guardian.getAuthorities())
        );

        when(guardianService.getDashboard(2L, LocalDate.of(2026, 11, 30), "MATH", null)).thenReturn(
                new GuardianDashboardResponse("Khoi", 7, LocalDate.of(2026, 11, 30), "MATH", 0, 0, 0, List.of())
        );

        mockMvc.perform(get("/api/guardian/dashboard")
                        .param("date", "2026-11-30")
                        .param("subjectCode", "MATH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedStudentName").value("Khoi"))
                .andExpect(jsonPath("$.data.grade").value(7))
                .andExpect(content().string(not(containsString("estimatedCostUsd"))))
                .andExpect(content().string(not(containsString("inputTokens"))));

        when(guardianService.getSubmissionDetail(2L, 999L))
                .thenThrow(new AppException(ErrorCode.GUARDIAN_NOT_LINKED));

        mockMvc.perform(get("/api/guardian/submissions/999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GRD_001"));

        when(guardianService.getSubmissionDetail(2L, 602L)).thenReturn(
                new GuardianSubmissionDetailResponse(
                        602L,
                        "Khoi",
                        LocalDate.of(2026, 11, 30),
                        SubjectCode.MATH,
                        "Toán",
                        StudyTaskType.REVIEW,
                        StudyTaskStatus.EVALUATED,
                        1,
                        List.of()
                )
        );

        mockMvc.perform(get("/api/guardian/submissions/602"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").value(602))
                .andExpect(jsonPath("$.data.studentName").value("Khoi"));

        when(guardianService.acknowledgeTask(eq(2L), eq(101L), any())).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeResponse(
                        101L, java.time.Instant.parse("2026-11-30T10:00:00Z"), "Cố gắng lên con!"
                )
        );
        mockMvc.perform(post("/api/guardian/tasks/101/acknowledge")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Cố gắng lên con!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(101))
                .andExpect(jsonPath("$.data.note").value("Cố gắng lên con!"));

        when(guardianService.getWeeklySummary(2L, LocalDate.of(2026, 11, 23), null)).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianWeeklySummaryResponse(
                        10L, "Khoi", 7, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29),
                        4, 3, 2, List.of(), List.of()
                )
        );
        mockMvc.perform(get("/api/guardian/weekly").param("weekStart", "2026-11-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedTasks").value(3))
                .andExpect(jsonPath("$.data.acknowledgedTasks").value(2));

        when(guardianService.exportExcelReport(2L, null, null, null)).thenReturn(new byte[]{'P', 'K', 3, 4});
        mockMvc.perform(get("/api/guardian/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-hoc-tap.xlsx")));

        when(guardianService.skipTask(eq(2L), eq(101L), any())).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskResponse(
                        101L, StudyTaskStatus.SKIPPED, "Không cần ôn"
                )
        );
        mockMvc.perform(post("/api/guardian/tasks/101/skip")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"skipped\":true,\"note\":\"Không cần ôn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(101))
                .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        when(guardianService.reviewSkipRequest(eq(2L), eq(101L), any())).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskResponse(
                        101L,
                        StudyTaskStatus.SKIPPED,
                        "Đồng ý cho con làm bài cô giao",
                        com.asc.studycompanion.module.study.entity.SkipRequestStatus.APPROVED,
                        "Làm bài tập cô giao",
                        java.time.Instant.parse("2026-11-26T01:00:00Z"),
                        java.time.Instant.parse("2026-11-26T01:10:00Z")
                )
        );
        mockMvc.perform(post("/api/guardian/tasks/101/skip-request/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"note\":\"Đồng ý cho con làm bài cô giao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(101))
                .andExpect(jsonPath("$.data.status").value("SKIPPED"))
                .andExpect(jsonPath("$.data.skipRequestStatus").value("APPROVED"));

        var assignedItem = new com.asc.studycompanion.module.guardian.dto.GuardianTaskItem(
                101L,
                null,
                LocalDate.of(2026, 11, 27),
                SubjectCode.MATH,
                "Toán",
                StudyTaskType.REVIEW,
                StudyTaskStatus.NOT_STARTED,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TaskAssignmentMode.CUSTOM_DRIVE,
                "Làm phiếu bài tập tuần 12",
                "Chụp ảnh nộp vào Drive",
                "https://drive.google.com/drive/folders/abc",
                java.time.Instant.parse("2026-11-26T02:00:00Z"),
                null,
                null,
                null
        );
        when(guardianService.assignCustomTask(eq(2L), eq(101L), any())).thenReturn(assignedItem);
        mockMvc.perform(post("/api/guardian/tasks/101/custom-task")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Làm phiếu bài tập tuần 12\",\"description\":\"Chụp ảnh nộp vào Drive\",\"driveFolderUrl\":\"https://drive.google.com/drive/folders/abc\",\"saveAsDefaultDriveFolder\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentMode").value("CUSTOM_DRIVE"))
                .andExpect(jsonPath("$.data.customDriveFolderUrl").value("https://drive.google.com/drive/folders/abc"));

        when(guardianService.verifyCustomTask(eq(2L), eq(101L), any())).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianTaskItem(
                        101L, null, LocalDate.of(2026, 11, 27), SubjectCode.MATH, "Toán",
                        StudyTaskType.REVIEW, StudyTaskStatus.EVALUATED, null, 0, null,
                        java.time.Instant.now(), "Đã kiểm tra Drive", null, null, null, null,
                        TaskAssignmentMode.CUSTOM_DRIVE, "Làm phiếu bài tập tuần 12", null,
                        "https://drive.google.com/drive/folders/abc", null, "Con đã up ảnh", null, java.time.Instant.now()
                )
        );
        mockMvc.perform(post("/api/guardian/tasks/101/custom-task/verify")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"note\":\"Đã kiểm tra Drive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EVALUATED"));

        when(guardianService.clearCustomTask(2L, 101L)).thenReturn(
                new com.asc.studycompanion.module.guardian.dto.GuardianTaskItem(
                        101L, null, LocalDate.of(2026, 11, 27), SubjectCode.MATH, "Toán",
                        StudyTaskType.REVIEW, StudyTaskStatus.NOT_STARTED, null, 0, null
                )
        );
        mockMvc.perform(delete("/api/guardian/tasks/101/custom-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentMode").value("STANDARD_AI"));
    }
}
