package com.asc.studycompanion.module.quiz.entity;

import com.asc.studycompanion.common.entity.BaseTimestampEntity;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily quiz session materialized once per student per calendar date (Asia/Ho_Chi_Minh).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "daily_quiz_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_quiz_sessions_student_date",
                columnNames = {"student_id", "session_date"}
        )
)
public class DailyQuizSession extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyQuizSessionStatus status = DailyQuizSessionStatus.IN_PROGRESS;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Builder.Default
    @Column(name = "answered_count", nullable = false)
    private int answeredCount = 0;

    @Builder.Default
    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Builder.Default
    @Column(name = "cycle_start_index", nullable = false)
    private int cycleStartIndex = 0;

    @Builder.Default
    @Column(name = "cycle_end_index", nullable = false)
    private int cycleEndIndex = 0;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<DailyQuizSessionItem> items = new ArrayList<>();
}
