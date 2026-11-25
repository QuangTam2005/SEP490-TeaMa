package com.asc.studycompanion.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Payload submitted by a Guardian to redeem an 8-char one-time link code.
 */
public record AcceptLinkCodeRequest(
        @NotBlank(message = "Mã liên kết không được để trống")
        @Size(min = 8, max = 8, message = "Mã liên kết phải gồm đúng 8 ký tự")
        String linkCode
) {
    public AcceptLinkCodeRequest {
        linkCode = linkCode != null ? linkCode.trim().toUpperCase(Locale.ROOT) : null;
    }
}
