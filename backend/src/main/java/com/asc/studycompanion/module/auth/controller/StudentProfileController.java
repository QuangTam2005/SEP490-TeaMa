package com.asc.studycompanion.module.auth.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.dto.UpdateStudentProfileRequest;
import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.service.AuthService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final AuthService authService;

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateStudentProfileRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        UserResponse updated = authService.updateStudentProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin học sinh thành công", updated));
    }
}
