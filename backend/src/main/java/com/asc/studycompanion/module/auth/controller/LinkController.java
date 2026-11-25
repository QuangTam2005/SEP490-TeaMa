package com.asc.studycompanion.module.auth.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.dto.AcceptLinkCodeRequest;
import com.asc.studycompanion.module.auth.dto.LinkCodeResponse;
import com.asc.studycompanion.module.auth.dto.LinkedChildResponse;
import com.asc.studycompanion.module.auth.dto.UnlinkRequest;
import com.asc.studycompanion.module.auth.service.LinkService;
import com.asc.studycompanion.security.UserPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<LinkCodeResponse>> getCurrentLink(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(linkService.getCurrentLink(principal.getId())));
    }

    @PostMapping("/code")
    public ResponseEntity<ApiResponse<LinkCodeResponse>> generateCode(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        LinkCodeResponse created = linkService.generateLinkCode(principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo mã liên kết thành công", created));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<LinkCodeResponse>> acceptCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AcceptLinkCodeRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        LinkCodeResponse linked = linkService.acceptLinkCode(principal.getId(), request.linkCode());
        return ResponseEntity.ok(ApiResponse.ok("Liên kết tài khoản thành công", linked));
    }

    @GetMapping("/children")
    public ResponseEntity<ApiResponse<List<LinkedChildResponse>>> getLinkedChildren(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(linkService.getLinkedChildren(principal.getId())));
    }

    @PostMapping("/unlink")
    public ResponseEntity<ApiResponse<Void>> unlink(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) UnlinkRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        linkService.unlink(principal.getId(), request != null ? request.studentId() : null);
        return ResponseEntity.ok(ApiResponse.okMessage("Đã hủy liên kết tài khoản"));
    }
}
