package com.asc.studycompanion.module.quiz;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.quiz.controller.StudentDailyQuizController;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizItemResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizSessionResponse;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerRequest;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerResponse;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;
import com.asc.studycompanion.module.quiz.entity.QuizOption;
import com.asc.studycompanion.module.quiz.service.DailyQuizService;
import com.asc.studycompanion.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentDailyQuizControllerTest {

    @Mock
    private DailyQuizService dailyQuizService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new StudentDailyQuizController(dailyQuizService))
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
    void getTodayAndSubmitAnswerReturnExpectedPayload() throws Exception {
        UserPrincipal principal = new UserPrincipal(10L, "student@example.com", "hash", "Hạnh Quyên", UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        StudentDailyQuizSessionResponse session = new StudentDailyQuizSessionResponse(
                5L,
                LocalDate.of(2026, 11, 28),
                DailyQuizSessionStatus.IN_PROGRESS,
                1, 0, 0, 0, 0, 1, null,
                List.of(new StudentDailyQuizItemResponse(
                        11L, 1, "1 + 1 = ?", "2", "3", "Toán", null, null, null, null, null
                ))
        );
        when(dailyQuizService.getTodayQuizForStudent(10L)).thenReturn(new StudentDailyQuizResponse(
                100L, LocalDate.of(2026, 11, 28), true, true, true, true, "Câu 1–1 / 1", session
        ));
        when(dailyQuizService.submitStudentAnswer(eq(10L), eq(5L), eq(11L), any()))
                .thenReturn(new SubmitDailyQuizAnswerResponse(
                        5L, 11L, QuizOption.A, QuizOption.A, true, "Cộng cơ bản",
                        1, 1, 1, 100, DailyQuizSessionStatus.COMPLETED, false, Instant.now()
                ));

        mockMvc.perform(get("/api/student/daily-quiz/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateLocked").value(true))
                .andExpect(jsonPath("$.data.session.items[0].correctOption").doesNotExist());

        mockMvc.perform(post("/api/student/daily-quiz/sessions/5/items/11/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitDailyQuizAnswerRequest(QuizOption.A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.gateLocked").value(false));
    }
}
