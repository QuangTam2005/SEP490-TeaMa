package com.asc.studycompanion.module.timetable.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.timetable.dto.TimetableResponse;
import com.asc.studycompanion.module.timetable.dto.UpdateTimetableRequest;
import com.asc.studycompanion.module.timetable.service.TimetableService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<ApiResponse<TimetableResponse>> getTimetable(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(timetableService.getTimetable(principal.getId(), studentId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<TimetableResponse>> updateTimetable(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody UpdateTimetableRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        TimetableResponse updated = timetableService.updateTimetable(principal.getId(), studentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lưu thời khóa biểu thành công", updated));
    }
}
