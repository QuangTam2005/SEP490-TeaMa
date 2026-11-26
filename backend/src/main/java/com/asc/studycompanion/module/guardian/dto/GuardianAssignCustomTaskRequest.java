package com.asc.studycompanion.module.guardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for a Guardian to replace a standard AI task with a custom homework task + Google Drive folder URL.
 */
public record GuardianAssignCustomTaskRequest(
        @NotBlank(message = "Vui lòng nhập tên nhiệm vụ thay thế")
        @Size(min = 5, max = 200, message = "Tên nhiệm vụ thay thế phải từ 5 đến 200 ký tự")
        String title,
        @Size(max = 500, message = "Hướng dẫn chi tiết tối đa 500 ký tự")
        String description,
        @NotBlank(message = "Vui lòng cung cấp link thư mục Google Drive để con nộp bài")
        @Size(max = 500, message = "Link Google Drive tối đa 500 ký tự")
        String driveFolderUrl,
        Boolean saveAsDefaultDriveFolder
) {
}
