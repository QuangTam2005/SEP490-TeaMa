package com.asc.studycompanion.common;

import com.asc.studycompanion.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodeTest {

    @Test
    void allErrorCodesMustHaveUniqueCodesAndValidMetadata() {
        Set<String> seenCodes = new HashSet<>();

        for (ErrorCode errorCode : ErrorCode.values()) {
            assertNotNull(errorCode.getCode(), "Code must not be null for " + errorCode.name());
            assertFalse(errorCode.getCode().isBlank(), "Code must not be blank for " + errorCode.name());
            assertTrue(seenCodes.add(errorCode.getCode()), "Duplicate error code found: " + errorCode.getCode());

            assertNotNull(errorCode.getHttpStatus(), "HttpStatus must not be null for " + errorCode.name());
            assertNotNull(errorCode.getDefaultMessage(), "Message must not be null for " + errorCode.name());
            assertFalse(errorCode.getDefaultMessage().isBlank(), "Message must not be blank for " + errorCode.name());
        }
    }
}
