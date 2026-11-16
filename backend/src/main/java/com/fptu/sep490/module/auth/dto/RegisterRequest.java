package com.fptu.sep490.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for student registration. Accepts any valid email domain.
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

        @Size(max = 20, message = "Mã sinh viên tối đa 20 ký tự")
        String studentCode,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone
) {
    public RegisterRequest {
        email = email != null ? email.trim() : null;
        fullName = fullName != null ? fullName.trim() : null;
        studentCode = (studentCode != null && !studentCode.isBlank()) ? studentCode.trim() : null;
        phone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
    }
}
