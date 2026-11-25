package com.asc.studycompanion.module.ai.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AI evaluation output mapped to ai_evaluations (600–1,000-char Vietnamese mentor feedback).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_evaluations")
public class AIEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accuracy", nullable = false, length = 30)
    private AccuracyLevel accuracy;

    @Enumerated(EnumType.STRING)
    @Column(name = "completeness", nullable = false, length = 30)
    private CompletenessLevel completeness;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 30)
    private OverallEvaluationStatus overallStatus;

    @Column(name = "feedback", nullable = false, length = 1000)
    private String feedback;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "incorrect_points", nullable = false, columnDefinition = "jsonb")
    private List<String> incorrectPoints = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_points", nullable = false, columnDefinition = "jsonb")
    private List<String> missingPoints = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false, length = 20)
    private ConfidenceLevel confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_sanitized_response", columnDefinition = "jsonb")
    private String rawSanitizedResponse;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SourceReference> sources = new ArrayList<>();

    @PrePersist
    protected void onPrePersist() {
        if (this.evaluatedAt == null) {
            this.evaluatedAt = Instant.now();
        }
    }
}
