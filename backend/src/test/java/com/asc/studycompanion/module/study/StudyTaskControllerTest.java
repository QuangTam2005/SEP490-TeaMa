package com.asc.studycompanion.module.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.study.controller.StudyTaskController;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.service.StudyTaskService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudyTaskControllerTest {

    @Mock
    private StudyTaskService studyTaskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new StudyTaskController(studyTaskService))
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
    }
}
