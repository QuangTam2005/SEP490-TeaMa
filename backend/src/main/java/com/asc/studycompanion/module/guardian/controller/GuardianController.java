package com.asc.studycompanion.module.guardian.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
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
