package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizSelectionMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateQuizSettingsRequest(
        @NotNull(message = "Vui lòng chọn trạng thái bật/tắt")
        Boolean enabled,
        @NotNull(message = "Số câu mỗi ngày không được để trống")
        @Min(value = 1, message = "Số câu mỗi ngày tối thiểu là 1")
        @Max(value = 50, message = "Số câu mỗi ngày tối đa là 50")
        Integer questionsPerDay,
        @NotEmpty(message = "Vui lòng chọn ít nhất 1 ngày trong tuần")
        List<@NotNull @Min(value = 1, message = "Thứ không hợp lệ") @Max(value = 7, message = "Thứ không hợp lệ") Integer> activeWeekdays,
        @NotNull(message = "Vui lòng chọn chế độ bóc câu hỏi")
        QuizSelectionMode selectionMode,
        @NotNull(message = "Tùy chọn xáo đáp án không được để trống")
        Boolean shuffleOptions,
        Boolean resetCursor
) {
}
