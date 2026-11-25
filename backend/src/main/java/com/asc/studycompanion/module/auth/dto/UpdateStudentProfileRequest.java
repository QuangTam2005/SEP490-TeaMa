package com.asc.studycompanion.module.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload allowing a Student to choose/update their current grade (1-12) and display name.
 */
public record UpdateStudentProfileRequest(
        @NotNull(message = "Khối lớp không được để trống")
        @Min(value = 1, message = "Khối lớp phải từ 1 đến 12")
        @Max(value = 12, message = "Khối lớp phải từ 1 đến 12")
        Integer grade,

        @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
        String displayName
) {
    public UpdateStudentProfileRequest {
        displayName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : null;
    }
}
