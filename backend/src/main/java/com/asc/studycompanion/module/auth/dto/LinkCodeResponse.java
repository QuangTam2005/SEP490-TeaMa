package com.asc.studycompanion.module.auth.dto;

import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;

import java.time.Instant;

/**
 * DTO matching Frontend LinkCodeResponse contract.
 */
public record LinkCodeResponse(
        String linkCode,
        Instant expiresAt,
        GuardianLinkStatus status,
        String linkedUserName
) {
}
