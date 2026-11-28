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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Single 2-option multiple choice question authored by a Guardian for a linked Student.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "guardian_quiz_questions")
public class GuardianQuizQuestion extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 1;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "option_a", nullable = false, length = 250)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 250)
    private String optionB;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_option", nullable = false, length = 10)
    private QuizOption correctOption;

    @Column(name = "explanation", length = 500)
    private String explanation;

    @Column(name = "topic_label", length = 80)
    private String topicLabel;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
