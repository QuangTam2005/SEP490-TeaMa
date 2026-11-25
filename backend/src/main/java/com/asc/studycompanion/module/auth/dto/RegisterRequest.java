package com.asc.studycompanion.module.auth.dto;

import com.asc.studycompanion.module.auth.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for STUDENT (grade 1-12) or GUARDIAN account registration. Accepts any valid email domain.
 */
public record RegisterRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
        String password,

        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
        String fullName,

        @NotNull(message = "Vai trò không được để trống")
        UserRole role,

        @Min(value = 1, message = "Khối lớp phải từ 1 đến 12")
        @Max(value = 12, message = "Khối lớp phải từ 1 đến 12")
        Integer grade
) {
    public RegisterRequest {
        email = email != null ? email.trim() : null;
        fullName = fullName != null ? fullName.trim() : null;
    }
}
