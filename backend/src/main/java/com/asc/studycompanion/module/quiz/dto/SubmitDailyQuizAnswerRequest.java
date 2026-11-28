package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizOption;
import jakarta.validation.constraints.NotNull;

public record SubmitDailyQuizAnswerRequest(
        @NotNull(message = "Vui lòng chọn đáp án A hoặc B")
        QuizOption selectedOption
) {
}
