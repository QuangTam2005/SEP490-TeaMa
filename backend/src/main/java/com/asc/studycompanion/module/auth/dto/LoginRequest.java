package com.asc.studycompanion.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for email/password login. Accepts any valid email domain.
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
    public LoginRequest {
        email = email != null ? email.trim() : null;
    }
}
