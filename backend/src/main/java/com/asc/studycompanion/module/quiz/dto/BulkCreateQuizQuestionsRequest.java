package com.asc.studycompanion.module.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateQuizQuestionsRequest(
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        @Size(max = 100, message = "Chỉ được thêm nhanh tối đa 100 câu mỗi lần")
        List<@NotNull(message = "Phần tử câu hỏi không được để trống") @Valid CreateQuizQuestionRequest> questions
) {
}
