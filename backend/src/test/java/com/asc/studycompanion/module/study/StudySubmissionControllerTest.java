package com.asc.studycompanion.module.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.study.controller.StudySubmissionController;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
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

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudySubmissionControllerTest {

    @Mock
    private StudySubmissionService studySubmissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new StudySubmissionController(studySubmissionService))
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
    void retryEvaluationRequiresAuthAndReturnsUpdatedOrRejectsInvalidState() throws Exception {
        mockMvc.perform(post("/api/student/submissions/601/retry-evaluation"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal student = new UserPrincipal(1L, "s@gmail.com", "h", "Khoi", UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null, student.getAuthorities())
        );

        when(studySubmissionService.retryEvaluation(1L, 601L)).thenReturn(
                new StudySubmissionResponse(
                        601L,
                        101L,
                        1,
                        true,
                        "Bài 1",
                        "x".repeat(900),
                        900,
                        Instant.now(),
                        null,
                        null
                )
        );

        mockMvc.perform(post("/api/student/submissions/601/retry-evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(601));

        when(studySubmissionService.retryEvaluation(1L, 602L))
                .thenThrow(new AppException(ErrorCode.AI_RETRY_NOT_ALLOWED));

        mockMvc.perform(post("/api/student/submissions/602/retry-evaluation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AI_004"));
    }
}
