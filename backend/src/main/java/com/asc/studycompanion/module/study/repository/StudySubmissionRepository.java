package com.asc.studycompanion.module.study.repository;

import com.asc.studycompanion.module.study.entity.StudySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudySubmissionRepository extends JpaRepository<StudySubmission, Long> {

    List<StudySubmission> findAllByTaskIdOrderByVersionDesc(Long taskId);

    Optional<StudySubmission> findFirstByTaskIdAndIsCurrentTrue(Long taskId);

    Optional<StudySubmission> findByIdempotencyKey(String idempotencyKey);

    boolean existsByTaskId(Long taskId);

    int countByTaskId(Long taskId);
}
