package com.asc.studycompanion.module.ai.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.dto.OpsUsageResponse;
import com.asc.studycompanion.module.ai.entity.AIUsageLog;
import com.asc.studycompanion.module.ai.repository.AIUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ops-only AI usage and cost telemetry endpoint.
 * Never linked or accessible from Student/Guardian UIs (BR-22).
 */
@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsUsageController {

    private final AIUsageLogRepository aiUsageLogRepository;

    @Value("${app.ops.admin-secret:}")
    private String opsAdminSecret = "";

    public void setOpsAdminSecret(String opsAdminSecret) {
        this.opsAdminSecret = opsAdminSecret;
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<OpsUsageResponse>> getUsageTelemetry(
            @RequestHeader(name = "X-Ops-Secret", required = false) String providedSecret,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (!isAuthorized(providedSecret)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không có quyền truy cập thông số vận hành AI");
        }

        LocalDate toDate = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = from != null ? from : toDate.minusDays(6);
        if (fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<AIUsageLog> logs = aiUsageLogRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(
                fromInstant, toInstant
        );

        int totalCalls = logs.size();
        int cacheHits = 0;
        int failedCalls = 0;
        long totalInput = 0;
        long totalOutput = 0;
        long totalSearch = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        Map<String, MutableModelStats> byModel = new LinkedHashMap<>();
        for (AIUsageLog log : logs) {
            boolean isHit = Boolean.TRUE.equals(log.getCacheHit());
            if (isHit) {
                cacheHits++;
            }
            if (!"SUCCEEDED".equalsIgnoreCase(log.getStatus()) && !"SUCCESS".equalsIgnoreCase(log.getStatus())) {
                failedCalls++;
            }
            int inTokens = log.getInputTokens() != null ? log.getInputTokens() : 0;
            int outTokens = log.getOutputTokens() != null ? log.getOutputTokens() : 0;
            int searches = log.getSearchCount() != null ? log.getSearchCount() : 0;
            BigDecimal cost = log.getEstimatedCostUsd() != null ? log.getEstimatedCostUsd() : BigDecimal.ZERO;

            totalInput += inTokens;
            totalOutput += outTokens;
            totalSearch += searches;
            totalCost = totalCost.add(cost);

            String key = log.getProvider() + "::" + log.getModel();
            MutableModelStats stats = byModel.computeIfAbsent(
                    key, k -> new MutableModelStats(log.getProvider(), log.getModel())
            );
            stats.calls++;
            if (isHit) {
                stats.cacheHits++;
            }
            stats.inputTokens += inTokens;
            stats.outputTokens += outTokens;
            stats.estimatedCostUsd = stats.estimatedCostUsd.add(cost);
        }

        List<OpsUsageResponse.ModelUsageBreakdown> breakdown = byModel.values().stream()
                .map(s -> new OpsUsageResponse.ModelUsageBreakdown(
                        s.provider, s.model, s.calls, s.cacheHits,
                        s.inputTokens, s.outputTokens, s.estimatedCostUsd
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new OpsUsageResponse(
                fromDate,
                toDate,
                totalCalls,
                cacheHits,
                failedCalls,
                totalInput,
                totalOutput,
                totalSearch,
                totalCost,
                breakdown
        )));
    }

    private boolean isAuthorized(String providedSecret) {
        if (opsAdminSecret == null || opsAdminSecret.isBlank() || providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                opsAdminSecret.trim().getBytes(StandardCharsets.UTF_8),
                providedSecret.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    private static final class MutableModelStats {
        final String provider;
        final String model;
        int calls = 0;
        int cacheHits = 0;
        long inputTokens = 0;
        long outputTokens = 0;
        BigDecimal estimatedCostUsd = BigDecimal.ZERO;

        MutableModelStats(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }
    }
}
