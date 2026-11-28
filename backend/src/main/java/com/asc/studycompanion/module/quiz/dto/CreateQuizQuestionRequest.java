package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuizQuestionRequest(
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        @Size(max = 500, message = "Nội dung câu hỏi tối đa 500 ký tự")
        String questionText,
        @NotBlank(message = "Đáp án A không được để trống")
        @Size(max = 250, message = "Đáp án A tối đa 250 ký tự")
        String optionA,
        @NotBlank(message = "Đáp án B không được để trống")
        @Size(max = 250, message = "Đáp án B tối đa 250 ký tự")
        String optionB,
        @NotNull(message = "Vui lòng chọn đáp án đúng (A hoặc B)")
        QuizOption correctOption,
        @Size(max = 500, message = "Giải thích tối đa 500 ký tự")
        String explanation,
        @Size(max = 80, message = "Chủ đề tối đa 80 ký tự")
        String topicLabel,
        Boolean active
) {
}
