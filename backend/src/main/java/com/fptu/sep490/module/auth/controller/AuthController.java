package com.fptu.sep490.module.auth.controller;

import com.fptu.sep490.common.dto.ApiResponse;
import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import com.fptu.sep490.module.auth.dto.AuthResponse;
import com.fptu.sep490.module.auth.dto.LoginRequest;
import com.fptu.sep490.module.auth.dto.RegisterRequest;
import com.fptu.sep490.module.auth.dto.UserResponse;
import com.fptu.sep490.module.auth.service.AuthService;
import com.fptu.sep490.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        UserResponse response = authService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
