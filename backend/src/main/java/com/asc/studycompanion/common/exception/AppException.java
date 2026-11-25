package com.asc.studycompanion.common.exception;

import lombok.Getter;

/**
 * Base runtime exception for all domain/business errors across modules.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}
