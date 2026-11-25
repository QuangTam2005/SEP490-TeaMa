package com.asc.studycompanion.module.study.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Immutable student submission version (v1..v3) mapped to study_submissions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "study_submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_study_submissions_task_version", columnNames = {"task_id", "version"}),
                @UniqueConstraint(name = "uq_study_submissions_idempotency", columnNames = {"idempotency_key"})
        }
)
public class StudySubmission {

    public static final int MAX_VERSIONS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private StudyTask task;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = Boolean.TRUE;

    @Column(name = "lesson_title", nullable = false, length = 255)
    private String lessonTitle;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "char_count", nullable = false)
    private Integer charCount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = Instant.now();
        }
        if (this.isCurrent == null) {
            this.isCurrent = Boolean.TRUE;
        }
    }
}
