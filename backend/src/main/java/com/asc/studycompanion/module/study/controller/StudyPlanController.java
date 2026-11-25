package com.asc.studycompanion.module.study.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.study.dto.NextDayStudyPlanResponse;
import com.asc.studycompanion.module.study.dto.StudentStreakResponse;
import com.asc.studycompanion.module.study.dto.StudyPlanHistoryItem;
import com.asc.studycompanion.module.study.service.StudyPlanService;
import com.asc.studycompanion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping("/plans/next-day")
    public ResponseEntity<ApiResponse<NextDayStudyPlanResponse>> getNextDayPlan(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(studyPlanService.getNextDayPlan(principal.getId())));
    }

    @GetMapping("/plans/history")
    public ResponseEntity<ApiResponse<List<StudyPlanHistoryItem>>> getPlanHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(studyPlanService.getPlanHistory(principal.getId(), from, to)));
    }

    @GetMapping("/streak")
    public ResponseEntity<ApiResponse<StudentStreakResponse>> getStreak(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(studyPlanService.getStreak(principal.getId())));
    }
}
