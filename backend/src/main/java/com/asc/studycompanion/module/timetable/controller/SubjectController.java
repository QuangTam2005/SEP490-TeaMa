package com.asc.studycompanion.module.timetable.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.timetable.dto.SubjectResponse;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getActiveSubjects(
            @RequestParam(required = false) Integer grade
    ) {
        if (grade != null && (grade < 1 || grade > 12)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Khối lớp phải từ 1 đến 12");
        }
        List<SubjectResponse> subjects = subjectRepository.findAllByIsActiveTrueOrderByIdAsc()
                .stream()
                .filter(s -> matchesGrade(s, grade))
                .map(s -> new SubjectResponse(
                        s.getId(),
                        s.getCode(),
                        s.getName(),
                        s.getMinGrade() != null ? s.getMinGrade() : 1,
                        s.getMaxGrade() != null ? s.getMaxGrade() : 12
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(subjects));
    }

    private boolean matchesGrade(Subject subject, Integer grade) {
        if (grade == null) {
            return true;
        }
        int min = subject.getMinGrade() != null ? subject.getMinGrade() : 1;
        int max = subject.getMaxGrade() != null ? subject.getMaxGrade() : 12;
        return grade >= min && grade <= max;
    }
}
