package com.asc.studycompanion.module.study.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
