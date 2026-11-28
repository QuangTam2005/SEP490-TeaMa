package com.asc.studycompanion.module.quiz;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.quiz.controller.GuardianQuizController;
import com.asc.studycompanion.module.quiz.dto.BulkCreateQuizQuestionsRequest;
import com.asc.studycompanion.module.quiz.dto.CreateQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSettingsResponse;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizStatsResponse;
import com.asc.studycompanion.module.quiz.dto.QuizQuestionResponse;
import com.asc.studycompanion.module.quiz.dto.UpdateQuizSettingsRequest;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;
import com.asc.studycompanion.module.quiz.entity.QuizOption;
import com.asc.studycompanion.module.quiz.entity.QuizSelectionMode;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GuardianQuizControllerTest {

    @Mock
    private DailyQuizService dailyQuizService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new GuardianQuizController(dailyQuizService))
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
    void getAndUpdateSettingsWorksForAuthenticatedGuardian() throws Exception {
        authenticateGuardian();
        GuardianQuizSettingsResponse response = new GuardianQuizSettingsResponse(
                100L, true, 10, List.of(1, 2, 3, 4, 5),
                QuizSelectionMode.SEQUENTIAL_ROTATION, false, 0, 20, 20, "Câu 1–10 / 20"
        );
        when(dailyQuizService.getGuardianSettings(20L, 100L)).thenReturn(response);
        when(dailyQuizService.updateGuardianSettings(eq(20L), eq(100L), any())).thenReturn(response);

        mockMvc.perform(get("/api/guardian/quiz/settings").param("studentId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionsPerDay").value(10))
                .andExpect(jsonPath("$.data.nextBatchLabel").value("Câu 1–10 / 20"));

        UpdateQuizSettingsRequest req = new UpdateQuizSettingsRequest(
                true, 10, List.of(1, 2, 3, 4, 5), QuizSelectionMode.SEQUENTIAL_ROTATION, false, false
        );
        mockMvc.perform(put("/api/guardian/quiz/settings")
                        .param("studentId", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void bulkCreateAndDeleteQuestionAndStatsReturnExpectedResponses() throws Exception {
        authenticateGuardian();
        QuizQuestionResponse q = new QuizQuestionResponse(
                1L, 1, "5 + 5 = ?", "10", "11", QuizOption.A, "10", "Toán", true, Instant.now(), Instant.now()
        );
        when(dailyQuizService.bulkCreateGuardianQuestions(eq(20L), eq(100L), any())).thenReturn(List.of(q));
        when(dailyQuizService.getGuardianStats(20L, 100L, 14)).thenReturn(new GuardianQuizStatsResponse(
                100L, "Hạnh Quyên", 1, 1, 1, 1, 1, 100, 1, DailyQuizSessionStatus.COMPLETED, List.of(), List.of()
        ));

        BulkCreateQuizQuestionsRequest bulkReq = new BulkCreateQuizQuestionsRequest(List.of(
                new CreateQuizQuestionRequest("5 + 5 = ?", "10", "11", QuizOption.A, "10", "Toán", true)
        ));

        mockMvc.perform(post("/api/guardian/quiz/questions/bulk")
                        .param("studentId", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].questionText").value("5 + 5 = ?"));

        mockMvc.perform(get("/api/guardian/quiz/stats")
                        .param("studentId", "100")
                        .param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageAccuracyPercent").value(100));

        mockMvc.perform(delete("/api/guardian/quiz/questions/1").param("studentId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void authenticateGuardian() {
        UserPrincipal principal = new UserPrincipal(20L, "guardian@example.com", "hash", "Phụ huynh", UserRole.GUARDIAN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
