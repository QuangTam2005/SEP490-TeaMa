package com.asc.studycompanion.module.timetable.dto;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Single timetable slot DTO aligned with Frontend TimetableEntryDto.
 */
public record TimetableEntryDto(
        @NotNull(message = "Ngày trong tuần không được để trống")
        @Min(value = 1, message = "Ngày trong tuần phải từ 1 (Thứ 2) đến 7 (Chủ nhật)")
        @Max(value = 7, message = "Ngày trong tuần phải từ 1 (Thứ 2) đến 7 (Chủ nhật)")
        Integer weekday,

        @NotNull(message = "Mã môn học không được để trống")
        SubjectCode subjectCode,

        @Size(max = 120, message = "Ghi chú tiết học tối đa 120 ký tự")
        String periodNote
) {
    public TimetableEntryDto {
        periodNote = (periodNote != null && !periodNote.isBlank()) ? periodNote.trim() : null;
    }
}
