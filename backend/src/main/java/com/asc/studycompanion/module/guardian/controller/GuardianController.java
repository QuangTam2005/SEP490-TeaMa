package com.asc.studycompanion.module.guardian.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianAssignCustomTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipReviewRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianTaskItem;
import com.asc.studycompanion.module.guardian.dto.GuardianVerifyCustomTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklySummaryResponse;
import com.asc.studycompanion.module.guardian.service.GuardianService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<GuardianDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                guardianService.getDashboard(principal.getId(), date, subjectCode, studentId)
        ));
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<GuardianSubmissionDetailResponse>> getSubmissionDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long submissionId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                guardianService.getSubmissionDetail(principal.getId(), submissionId)
        ));
    }

    @PostMapping("/tasks/{taskId}/acknowledge")
    public ResponseEntity<ApiResponse<GuardianAcknowledgeResponse>> acknowledgeTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody(required = false) GuardianAcknowledgeRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianAcknowledgeResponse ack = guardianService.acknowledgeTask(principal.getId(), taskId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã xác nhận xem bài của con", ack));
    }

    @PostMapping("/tasks/{taskId}/skip")
    public ResponseEntity<ApiResponse<GuardianSkipTaskResponse>> skipTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody(required = false) GuardianSkipTaskRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianSkipTaskResponse res = guardianService.skipTask(principal.getId(), taskId, request);
        String msg = res.status() == com.asc.studycompanion.module.study.entity.StudyTaskStatus.SKIPPED
                ? "Đã đánh dấu bỏ qua bài học cho con"
                : "Đã khôi phục bài học cho con";
        return ResponseEntity.ok(ApiResponse.ok(msg, res));
    }

    @PostMapping("/tasks/{taskId}/skip-request/review")
    public ResponseEntity<ApiResponse<GuardianSkipTaskResponse>> reviewSkipRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody GuardianSkipReviewRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianSkipTaskResponse res = guardianService.reviewSkipRequest(principal.getId(), taskId, request);
        String msg = res.status() == com.asc.studycompanion.module.study.entity.StudyTaskStatus.SKIPPED
                ? "Đã đồng ý cho con bỏ qua bài học"
                : "Đã từ chối yêu cầu xin bỏ qua của con";
        return ResponseEntity.ok(ApiResponse.ok(msg, res));
    }

    @PostMapping("/tasks/{taskId}/custom-task")
    public ResponseEntity<ApiResponse<GuardianTaskItem>> assignCustomTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody GuardianAssignCustomTaskRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianTaskItem updated = guardianService.assignCustomTask(principal.getId(), taskId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã giao nhiệm vụ thay thế kèm thư mục Google Drive cho con", updated));
    }

    @DeleteMapping("/tasks/{taskId}/custom-task")
    public ResponseEntity<ApiResponse<GuardianTaskItem>> clearCustomTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianTaskItem updated = guardianService.clearCustomTask(principal.getId(), taskId);
        return ResponseEntity.ok(ApiResponse.ok("Đã chuyển nhiệm vụ về chế độ AI mặc định", updated));
    }

    @PostMapping("/tasks/{taskId}/custom-task/verify")
    public ResponseEntity<ApiResponse<GuardianTaskItem>> verifyCustomTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody GuardianVerifyCustomTaskRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        GuardianTaskItem updated = guardianService.verifyCustomTask(principal.getId(), taskId, request);
        String msg = updated.status() == com.asc.studycompanion.module.study.entity.StudyTaskStatus.EVALUATED
                ? "Đã xác nhận con hoàn thành bài nộp trên Drive"
                : "Đã gửi lời nhắc con bổ sung bài làm trên Drive";
        return ResponseEntity.ok(ApiResponse.ok(msg, updated));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<GuardianWeeklySummaryResponse>> getWeeklySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                guardianService.getWeeklySummary(principal.getId(), weekStart, studentId)
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        byte[] xlsxBytes = guardianService.exportExcelReport(principal.getId(), from, to, studentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao-cao-hoc-tap.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(xlsxBytes);
    }
}
