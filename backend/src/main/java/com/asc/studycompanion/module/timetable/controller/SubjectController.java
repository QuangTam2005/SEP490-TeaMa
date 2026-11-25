package com.asc.studycompanion.module.timetable.controller;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.module.timetable.dto.SubjectResponse;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getActiveSubjects() {
        List<SubjectResponse> subjects = subjectRepository.findAllByIsActiveTrueOrderByIdAsc()
                .stream()
                .map(s -> new SubjectResponse(s.getId(), s.getCode(), s.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(subjects));
    }
}
