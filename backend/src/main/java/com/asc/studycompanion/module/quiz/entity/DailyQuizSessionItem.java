package com.asc.studycompanion.module.quiz.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Immutable snapshot of one drawn question and the student's submitted answer inside a DailyQuizSession.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "daily_quiz_session_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_quiz_session_items_order",
                columnNames = {"session_id", "item_order"}
        )
)
public class DailyQuizSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DailyQuizSession session;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "display_option_a", nullable = false, length = 250)
    private String displayOptionA;

    @Column(name = "display_option_b", nullable = false, length = 250)
    private String displayOptionB;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_display_option", nullable = false, length = 10)
    private QuizOption correctDisplayOption;

    @Column(name = "explanation", length = 500)
    private String explanation;

    @Column(name = "topic_label", length = 80)
    private String topicLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_option", length = 10)
    private QuizOption selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "answered_at")
    private Instant answeredAt;
}
