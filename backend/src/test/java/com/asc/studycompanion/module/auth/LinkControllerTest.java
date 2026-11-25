package com.asc.studycompanion.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.controller.LinkController;
import com.asc.studycompanion.module.auth.dto.LinkCodeResponse;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.service.LinkService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LinkControllerTest {

    @Mock
    private LinkService linkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new LinkController(linkService))
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
    void endpointsRequireAuthAndSupportStudentGenerateAndGuardianAccept() throws Exception {
        mockMvc.perform(get("/api/links/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal studentPrincipal = new UserPrincipal(1L, "s@gmail.com", "h", "Khoi", UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentPrincipal, null, studentPrincipal.getAuthorities())
        );

        when(linkService.generateLinkCode(1L)).thenReturn(
                new LinkCodeResponse("KHOI2026", Instant.parse("2026-11-26T02:00:00Z"), GuardianLinkStatus.PENDING, null)
        );

        mockMvc.perform(post("/api/links/code"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.linkCode").value("KHOI2026"));

        UserPrincipal guardianPrincipal = new UserPrincipal(2L, "g@gmail.com", "h", "Hang", UserRole.GUARDIAN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guardianPrincipal, null, guardianPrincipal.getAuthorities())
        );

        mockMvc.perform(post("/api/links/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"linkCode\":\"SHORT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"))
                .andExpect(jsonPath("$.data.linkCode").exists());

        when(linkService.acceptLinkCode(2L, "KHOI2026")).thenReturn(
                new LinkCodeResponse("KHOI2026", Instant.parse("2026-11-26T02:00:00Z"), GuardianLinkStatus.ACTIVE, "Khoi")
        );

        mockMvc.perform(post("/api/links/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"linkCode\":\" khoi2026 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.linkedUserName").value("Khoi"));
    }
}
