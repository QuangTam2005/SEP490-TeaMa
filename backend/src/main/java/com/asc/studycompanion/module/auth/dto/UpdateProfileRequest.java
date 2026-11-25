package com.asc.studycompanion.module.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Unified profile update payload supporting both STUDENT (fullName + optional grade)
 * and GUARDIAN (fullName).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
        String fullName,

        @Min(value = 1, message = "Khối lớp phải từ 1 đến 12")
        @Max(value = 12, message = "Khối lớp phải từ 1 đến 12")
        Integer grade
) {
    public UpdateProfileRequest {
        fullName = fullName == null ? null : fullName.trim();
    }
}
