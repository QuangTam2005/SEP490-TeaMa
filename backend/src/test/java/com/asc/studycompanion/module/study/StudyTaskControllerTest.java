package com.asc.studycompanion.module.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.study.controller.StudyTaskController;
import com.asc.studycompanion.module.study.dto.StudentCustomSubmitRequest;
import com.asc.studycompanion.module.study.dto.StudentSkipRequestDto;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudyTaskControllerTest {

    @Mock
    private StudyTaskService studyTaskService;
    @Mock
    private StudySubmissionService studySubmissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new StudyTaskController(studyTaskService, studySubmissionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTaskDetailRequiresAuthAndReturnsTaskOr404() throws Exception {
        mockMvc.perform(get("/api/student/tasks/101"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal student = new UserPrincipal(1L, "s@gmail.com", "h", "Khoi", UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null, student.getAuthorities())
        );

        when(studyTaskService.getTaskDetail(1L, 999L))
                .thenThrow(new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));

        mockMvc.perform(get("/api/student/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STUDY_001"));

        StudyTaskSummaryResponse summary = new StudyTaskSummaryResponse(
                101L,
                SubjectCode.MATH,
                "Toán",
                StudyTaskType.REVIEW,
                StudyTaskStatus.NOT_STARTED,
                0,
                2,
                null,
                null
        );
        when(studyTaskService.getTaskDetail(1L, 101L)).thenReturn(
                new StudyTaskDetailResponse(summary, LocalDate.of(2026, 11, 30), false, List.of())
        );

        mockMvc.perform(get("/api/student/tasks/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.subjectCode").value("MATH"))
                .andExpect(jsonPath("$.data.task.maxRevisions").value(2));

        when(studySubmissionService.submitTask(eq(1L), eq(101L), any(SubmitStudyRequest.class), eq("idem-http-1")))
                .thenReturn(new StudySubmissionResponse(
                        501L,
                        101L,
                        1,
                        true,
                        "Bài 1",
                        "x".repeat(900),
                        900,
                        Instant.now(),
                        null,
                        null
                ));

        mockMvc.perform(post("/api/student/tasks/101/submissions")
                        .header("Idempotency-Key", "idem-http-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonTitle\":\"Bài 1\",\"content\":\"" + "x".repeat(900) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.charCount").value(900));

        mockMvc.perform(post("/api/student/tasks/101/skip-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ng\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        when(studyTaskService.requestSkip(eq(1L), eq(101L), any(StudentSkipRequestDto.class)))
                .thenReturn(new StudyTaskSummaryResponse(
                        101L, SubjectCode.MATH, "Toán", StudyTaskType.REVIEW, StudyTaskStatus.NOT_STARTED,
                        0, 2, null, null, null, null,
                        SkipRequestStatus.PENDING, "Làm bài tập thầy cô giao", Instant.now(), null
                ));

        mockMvc.perform(post("/api/student/tasks/101/skip-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Làm bài tập thầy cô giao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skipRequestStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.skipRequestReason").value("Làm bài tập thầy cô giao"));

        when(studyTaskService.cancelSkipRequest(1L, 101L))
                .thenReturn(summary);

        mockMvc.perform(delete("/api/student/tasks/101/skip-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101));

        when(studyTaskService.submitCustomTask(eq(1L), eq(101L), nullable(StudentCustomSubmitRequest.class)))
                .thenReturn(new StudyTaskSummaryResponse(
                        101L, SubjectCode.MATH, "Toán", StudyTaskType.REVIEW, StudyTaskStatus.SUBMITTED,
                        0, 2, null, null, null, null, null, null, null, null,
                        TaskAssignmentMode.CUSTOM_DRIVE, "Làm phiếu Toán", null, "https://drive.google.com/drive/folders/abc",
                        Instant.now(), "Con đã tải ảnh lên Drive", null, Instant.now()
                ));

        mockMvc.perform(post("/api/student/tasks/101/custom-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionNote\":\"Con đã tải ảnh lên Drive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.assignmentMode").value("CUSTOM_DRIVE"));

        // 1-click confirmation without body or note succeeds cleanly
        mockMvc.perform(post("/api/student/tasks/101/custom-submission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        // Negative validation on custom-submission (>300-char note & >500-char URL)
        mockMvc.perform(post("/api/student/tasks/101/custom-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionNote\":\"" + "a".repeat(301) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        mockMvc.perform(post("/api/student/tasks/101/custom-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionNote\":\"Con đã tải ảnh lên Drive\",\"driveFileUrl\":\"https://drive.google.com/" + "a".repeat(480) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        // Anti-IDOR / wrong role 403 AUTH_004 propagated cleanly on skip-request & custom-submission
        UserPrincipal guardian = new UserPrincipal(2L, "g@gmail.com", "h", "Hang", UserRole.GUARDIAN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guardian, null, guardian.getAuthorities())
        );
        when(studyTaskService.requestSkip(eq(2L), eq(101L), any(StudentSkipRequestDto.class)))
                .thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        when(studyTaskService.cancelSkipRequest(2L, 101L))
                .thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        when(studyTaskService.submitCustomTask(eq(2L), eq(101L), any(StudentCustomSubmitRequest.class)))
                .thenThrow(new AppException(ErrorCode.ACCESS_DENIED));

        mockMvc.perform(post("/api/student/tasks/101/skip-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Làm bài tập thầy cô giao\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        mockMvc.perform(delete("/api/student/tasks/101/skip-request"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        mockMvc.perform(post("/api/student/tasks/101/custom-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionNote\":\"Con đã tải ảnh lên Drive\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        // Unauthenticated requests on skip/custom endpoints return 401 AUTH_001
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/student/tasks/101/skip-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Làm bài tập thầy cô giao\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
        mockMvc.perform(delete("/api/student/tasks/101/skip-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
        mockMvc.perform(post("/api/student/tasks/101/custom-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionNote\":\"Con đã tải ảnh lên Drive\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
    }
}
