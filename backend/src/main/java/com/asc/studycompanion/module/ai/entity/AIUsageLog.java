package com.asc.studycompanion.module.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Internal AI cost and telemetry log mapped to ai_usage_logs (never exposed on Guardian Dashboard).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_usage_logs")
public class AIUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 80)
    private String model;

    @Builder.Default
    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens = 0;

    @Builder.Default
    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens = 0;

    @Builder.Default
    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 0;

    @Builder.Default
    @Column(name = "cache_hit", nullable = false)
    private Boolean cacheHit = Boolean.FALSE;

    @Builder.Default
    @Column(name = "estimated_cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
