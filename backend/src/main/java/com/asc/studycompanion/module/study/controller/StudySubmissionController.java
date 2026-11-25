package com.asc.studycompanion.module.study.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/submissions")
@RequiredArgsConstructor
public class StudySubmissionController {

    private final StudySubmissionService studySubmissionService;

    @PostMapping("/{submissionId}/retry-evaluation")
    public ResponseEntity<ApiResponse<StudySubmissionResponse>> retryEvaluation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long submissionId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        StudySubmissionResponse response =
                studySubmissionService.retryEvaluation(principal.getId(), submissionId);
        return ResponseEntity.ok(ApiResponse.ok("Đã đưa bài nộp vào hàng đợi đánh giá lại", response));
    }
}
