package com.asc.studycompanion.module.study.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.study.dto.StudentCustomSubmitRequest;
import com.asc.studycompanion.module.study.dto.StudentSkipRequestDto;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/tasks")
@RequiredArgsConstructor
public class StudyTaskController {

    private final StudyTaskService studyTaskService;
    private final StudySubmissionService studySubmissionService;

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<StudyTaskDetailResponse>> getTaskDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(studyTaskService.getTaskDetail(principal.getId(), taskId)));
    }

    @PostMapping("/{taskId}/submissions")
    public ResponseEntity<ApiResponse<StudySubmissionResponse>> submitTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody SubmitStudyRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StudySubmissionResponse created = studySubmissionService.submitTask(
                principal.getId(),
                taskId,
                request,
                headerIdempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Nộp bài thành công", created));
    }

    @PostMapping("/{taskId}/skip-request")
    public ResponseEntity<ApiResponse<StudyTaskSummaryResponse>> requestSkip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody StudentSkipRequestDto request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StudyTaskSummaryResponse updated = studyTaskService.requestSkip(principal.getId(), taskId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi xin phép bỏ qua tới phụ huynh", updated));
    }

    @DeleteMapping("/{taskId}/skip-request")
    public ResponseEntity<ApiResponse<StudyTaskSummaryResponse>> cancelSkipRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StudyTaskSummaryResponse updated = studyTaskService.cancelSkipRequest(principal.getId(), taskId);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy yêu cầu xin bỏ qua", updated));
    }

    @PostMapping("/{taskId}/custom-submission")
    public ResponseEntity<ApiResponse<StudyTaskSummaryResponse>> submitCustomTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody(required = false) StudentCustomSubmitRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StudyTaskSummaryResponse updated = studyTaskService.submitCustomTask(principal.getId(), taskId, request);
        String msg = updated != null && updated.customDriveFolderUrl() != null
                ? "Đã xác nhận nộp bài lên Google Drive"
                : "Đã xác nhận hoàn thành nhiệm vụ";
        return ResponseEntity.ok(ApiResponse.ok(msg, updated));
    }
}
