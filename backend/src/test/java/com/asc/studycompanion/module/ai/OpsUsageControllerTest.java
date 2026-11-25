package com.asc.studycompanion.module.ai;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.ai.controller.OpsUsageController;
import com.asc.studycompanion.module.ai.entity.AIUsageLog;
import com.asc.studycompanion.module.ai.repository.AIUsageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpsUsageControllerTest {

    @Mock
    private AIUsageLogRepository aiUsageLogRepository;

    private OpsUsageController opsUsageController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        opsUsageController = new OpsUsageController(aiUsageLogRepository);
        opsUsageController.setOpsAdminSecret("super-secret-ops-token");

        mockMvc = MockMvcBuilders.standaloneSetup(opsUsageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void rejectsMissingOrWrongSecretWith403AndReturnsAggregatedUsageWhenAuthorized() throws Exception {
        mockMvc.perform(get("/api/ops/usage"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        mockMvc.perform(get("/api/ops/usage").header("X-Ops-Secret", "wrong-secret"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));

        when(aiUsageLogRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(
                AIUsageLog.builder()
                        .id(1L)
                        .jobId(10L)
                        .provider("ckey")
                        .model("xztimee/laguna-xs-2.1")
                        .inputTokens(1200)
                        .outputTokens(350)
                        .searchCount(2)
                        .cacheHit(false)
                        .estimatedCostUsd(new BigDecimal("0.001250"))
                        .latencyMs(820)
                        .status("SUCCESS")
                        .createdAt(Instant.now())
                        .build(),
                AIUsageLog.builder()
                        .id(2L)
                        .jobId(11L)
                        .provider("ckey")
                        .model("xztimee/laguna-xs-2.1")
                        .inputTokens(900)
                        .outputTokens(300)
                        .searchCount(0)
                        .cacheHit(true)
                        .estimatedCostUsd(new BigDecimal("0.000800"))
                        .latencyMs(410)
                        .status("SUCCESS")
                        .createdAt(Instant.now())
                        .build()
        ));

        mockMvc.perform(get("/api/ops/usage")
                        .header("X-Ops-Secret", "super-secret-ops-token")
                        .param("from", "2026-11-20")
                        .param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCalls").value(2))
                .andExpect(jsonPath("$.data.cacheHits").value(1))
                .andExpect(jsonPath("$.data.totalInputTokens").value(2100))
                .andExpect(jsonPath("$.data.totalOutputTokens").value(650))
                .andExpect(jsonPath("$.data.totalSearchCount").value(2))
                .andExpect(jsonPath("$.data.byModel[0].model").value("xztimee/laguna-xs-2.1"));
    }
}
