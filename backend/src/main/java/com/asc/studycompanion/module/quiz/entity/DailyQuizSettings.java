package com.asc.studycompanion.module.quiz.entity;

import com.asc.studycompanion.common.entity.BaseTimestampEntity;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Per-student Guardian configuration governing when and how daily 2-option quiz batches appear.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_quiz_settings")
public class DailyQuizSettings extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentProfile student;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Builder.Default
    @Column(name = "questions_per_day", nullable = false)
    private int questionsPerDay = 10;

    @Builder.Default
    @Column(name = "active_weekdays", nullable = false, length = 20)
    private String activeWeekdays = "1,2,3,4,5,6,7";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 30)
    private QuizSelectionMode selectionMode = QuizSelectionMode.SEQUENTIAL_ROTATION;

    @Builder.Default
    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions = false;

    @Builder.Default
    @Column(name = "rotation_cursor", nullable = false)
    private int rotationCursor = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;
}
