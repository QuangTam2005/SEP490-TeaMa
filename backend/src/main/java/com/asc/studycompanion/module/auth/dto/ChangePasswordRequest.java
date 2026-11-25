package com.asc.studycompanion.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to change the authenticated user's password.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự")
        String newPassword
) {
}
