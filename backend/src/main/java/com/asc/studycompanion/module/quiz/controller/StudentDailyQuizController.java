package com.asc.studycompanion.module.quiz.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizResponse;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerRequest;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerResponse;
import com.asc.studycompanion.module.quiz.service.DailyQuizService;
import com.asc.studycompanion.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/daily-quiz")
@RequiredArgsConstructor
public class StudentDailyQuizController {

    private final DailyQuizService dailyQuizService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<StudentDailyQuizResponse>> getTodayQuiz(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.getTodayQuizForStudent(principal.getId())
        ));
    }

    @PostMapping("/sessions/{sessionId}/items/{itemId}/answer")
    public ResponseEntity<ApiResponse<SubmitDailyQuizAnswerResponse>> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId,
            @PathVariable Long itemId,
            @Valid @RequestBody SubmitDailyQuizAnswerRequest request
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                dailyQuizService.submitStudentAnswer(principal.getId(), sessionId, itemId, request)
        ));
    }
}
