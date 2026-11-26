package com.asc.studycompanion.module.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.study.controller.StudyTaskController;
import com.asc.studycompanion.module.study.dto.StudentSkipRequestDto;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
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
    }
}
