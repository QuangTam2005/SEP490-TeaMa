package com.asc.studycompanion.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.controller.AuthController;
import com.asc.studycompanion.module.auth.dto.AuthResponse;
import com.asc.studycompanion.module.auth.dto.LoginRequest;
import com.asc.studycompanion.module.auth.dto.RegisterRequest;
import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
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
    void registerTrimsWhitespaceEmailAndReturns201() throws Exception {
        UserResponse user = new UserResponse(
                1L,
                "hocsinh@gmail.com",
                "Nguyen Minh Khoi",
                UserRole.STUDENT,
                6,
                null,
                Instant.parse("2026-11-25T02:00:00Z")
        );
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "Bearer", 86_400_000L, user));

        String body = """
                {
                  "email": "  hocsinh@gmail.com  ",
                  "password": "password123",
                  "fullName": "  Nguyen Minh Khoi ",
                  "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("hocsinh@gmail.com"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.grade").value(6));
    }

    @Test
    void registerValidationRejectsInvalidEmailAndMissingRole() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "password": "123",
                  "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL_001"))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.role").exists());
    }

    @Test
    void loginReturns200OnSuccessAnd401OnInvalidCredentials() throws Exception {
        UserResponse user = new UserResponse(
                2L,
                "phuhuynh@outlook.com",
                "Nguyen Thu Hang",
                UserRole.GUARDIAN,
                null,
                "Nguyen Minh Khoi",
                Instant.now()
        );
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-ok", "Bearer", 86_400_000L, user))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        String body = """
                {
                  "email": " phuhuynh@outlook.com ",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-ok"))
                .andExpect(jsonPath("$.data.user.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.user.linkedStudentName").value("Nguyen Minh Khoi"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_002"));
    }

    @Test
    void getMeReturns401WhenUnauthenticatedAnd200WhenPrincipalPresent() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal principal = new UserPrincipal(
                12L,
                "hocsinh@gmail.com",
                "hash",
                "Nguyen Minh Khoi",
                UserRole.STUDENT
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        UserResponse user = new UserResponse(
                12L,
                "hocsinh@gmail.com",
                "Nguyen Minh Khoi",
                UserRole.STUDENT,
                6,
                null,
                Instant.now()
        );
        when(authService.getCurrentUser(12L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.email").value("hocsinh@gmail.com"))
                .andExpect(jsonPath("$.data.grade").value(6));
    }
}
