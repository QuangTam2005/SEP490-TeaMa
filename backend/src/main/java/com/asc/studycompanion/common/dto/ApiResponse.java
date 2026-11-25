package com.asc.studycompanion.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.asc.studycompanion.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified REST API response envelope shared across all endpoints.
 * JSON shape: { success, message, data, errorCode, timestamp }.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Thành công", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errorCode(null)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> okMessage(String message) {
        return ok(message, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getDefaultMessage(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String customMessage) {
        return error(errorCode, customMessage, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage, T data) {
        String resolvedMessage = (customMessage != null && !customMessage.isBlank())
                ? customMessage
                : errorCode.getDefaultMessage();

        return ApiResponse.<T>builder()
                .success(false)
                .message(resolvedMessage)
                .data(data)
                .errorCode(errorCode.getCode())
                .timestamp(Instant.now())
                .build();
    }
}
