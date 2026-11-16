package com.fptu.sep490.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import com.fptu.sep490.common.exception.GlobalExceptionHandler;
import com.fptu.sep490.module.auth.controller.AuthController;
import com.fptu.sep490.module.auth.dto.AuthResponse;
import com.fptu.sep490.module.auth.dto.LoginRequest;
import com.fptu.sep490.module.auth.dto.RegisterRequest;
import com.fptu.sep490.module.auth.dto.UserResponse;
import com.fptu.sep490.module.auth.service.AuthService;
import com.fptu.sep490.security.UserPrincipal;
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
                "user@gmail.com",
                "Nguyen Van A",
                "SE180001",
                null,
                null,
                Instant.parse("2026-11-16T05:00:00Z")
        );
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "Bearer", 86_400_000L, user));

        String body = """
                {
                  "email": "  user@gmail.com  ",
                  "password": "password123",
                  "fullName": "  Nguyen Van A ",
                  "studentCode": "SE180001"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("user@gmail.com"));
    }

    @Test
    void registerValidationRejectsInvalidEmailAndShortPassword() throws Exception {
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
                .andExpect(jsonPath("$.data.fullName").exists());
    }

    @Test
    void loginReturns200OnSuccessAnd401OnInvalidCredentials() throws Exception {
        UserResponse user = new UserResponse(2L, "dev@outlook.com", "Tran B", null, null, null, Instant.now());
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-ok", "Bearer", 86_400_000L, user))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        String body = """
                {
                  "email": " dev@outlook.com ",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-ok"));

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

        UserPrincipal principal = new UserPrincipal(12L, "team@gmail.com", "hash", "Pham C");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        UserResponse user = new UserResponse(12L, "team@gmail.com", "Pham C", "SE180999", null, null, Instant.now());
        when(authService.getCurrentUser(12L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.email").value("team@gmail.com"));
    }
}
