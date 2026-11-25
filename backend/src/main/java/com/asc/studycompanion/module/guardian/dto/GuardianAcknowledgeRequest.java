package com.asc.studycompanion.module.guardian.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload when a Guardian marks a student's task as viewed and optionally leaves an encouraging note.
 */
public record GuardianAcknowledgeRequest(
        @Size(max = 300, message = "Lời nhắn động viên tối đa 300 ký tự")
        String note
) {
}
