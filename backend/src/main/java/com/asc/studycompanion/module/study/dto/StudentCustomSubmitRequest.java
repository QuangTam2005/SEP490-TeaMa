package com.asc.studycompanion.module.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Student confirmation payload after uploading their custom assignment to the Guardian's Google Drive folder.
 */
public record StudentCustomSubmitRequest(
        @NotBlank(message = "Con hãy ghi chú ngắn kết quả đã nộp vào Drive nhé")
        @Size(min = 5, max = 300, message = "Ghi chú nộp bài phải từ 5 đến 300 ký tự")
        String submissionNote,
        @Size(max = 500, message = "Link file Google Drive tối đa 500 ký tự")
        String driveFileUrl
) {
}
