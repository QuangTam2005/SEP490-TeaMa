package com.asc.studycompanion.module.ai.repository;

import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AIEvaluationRepository extends JpaRepository<AIEvaluation, Long> {

    Optional<AIEvaluation> findBySubmissionId(Long submissionId);

    List<AIEvaluation> findBySubmissionIdIn(Collection<Long> submissionIds);
}
