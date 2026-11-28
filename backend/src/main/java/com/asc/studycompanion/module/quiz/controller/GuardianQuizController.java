package com.asc.studycompanion.module.quiz.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.quiz.dto.BulkCreateQuizQuestionsRequest;
import com.asc.studycompanion.module.quiz.dto.CreateQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSessionDetailResponse;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSettingsResponse;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizStatsResponse;
import com.asc.studycompanion.module.quiz.dto.MoveQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.QuizQuestionResponse;
import com.asc.studycompanion.module.quiz.dto.UpdateQuizSettingsRequest;
import com.asc.studycompanion.module.quiz.service.DailyQuizService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guardian/quiz")
@RequiredArgsConstructor
public class GuardianQuizController {

    private final DailyQuizService dailyQuizService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<GuardianQuizSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.getGuardianSettings(principal.getId(), studentId)
        ));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<GuardianQuizSettingsResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody UpdateQuizSettingsRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã lưu cài đặt trắc nghiệm đầu ngày",
                dailyQuizService.updateGuardianSettings(principal.getId(), studentId, request)
        ));
    }

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> listQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.listGuardianQuestions(principal.getId(), studentId)
        ));
    }

    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody CreateQuizQuestionRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Đã thêm câu hỏi mới vào ngân hàng",
                dailyQuizService.createGuardianQuestion(principal.getId(), studentId, request)
        ));
    }

    @PostMapping("/questions/bulk")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> bulkCreateQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody BulkCreateQuizQuestionsRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        List<QuizQuestionResponse> created = dailyQuizService.bulkCreateGuardianQuestions(
                principal.getId(),
                studentId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                String.format("Đã thêm nhanh %d câu hỏi vào ngân hàng", created.size()),
                created
        ));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> updateQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody CreateQuizQuestionRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật câu hỏi",
                dailyQuizService.updateGuardianQuestion(principal.getId(), questionId, studentId, request)
        ));
    }

    @PostMapping("/questions/{questionId}/move")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> moveQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId,
            @RequestParam(required = false) Long studentId,
            @Valid @RequestBody MoveQuizQuestionRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.moveGuardianQuestion(principal.getId(), questionId, studentId, request)
        ));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        dailyQuizService.deleteGuardianQuestion(principal.getId(), questionId, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa câu hỏi", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<GuardianQuizStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer days
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.getGuardianStats(principal.getId(), studentId, days)
        ));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<GuardianQuizSessionDetailResponse>> getSessionDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long studentId
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.getGuardianSessionDetail(principal.getId(), sessionId, studentId)
        ));
    }
}
