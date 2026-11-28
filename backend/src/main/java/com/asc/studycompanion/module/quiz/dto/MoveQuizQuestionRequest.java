package com.asc.studycompanion.module.quiz.dto;

import jakarta.validation.constraints.NotNull;

public record MoveQuizQuestionRequest(
        @NotNull(message = "Hướng di chuyển không được để trống")
        MoveDirection direction
) {
    public enum MoveDirection {
        UP,
        DOWN
    }
}
