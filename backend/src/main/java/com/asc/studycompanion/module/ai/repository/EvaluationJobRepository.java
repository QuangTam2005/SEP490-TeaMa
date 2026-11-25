package com.asc.studycompanion.module.ai.repository;

import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, Long> {

    Optional<EvaluationJob> findBySubmissionId(Long submissionId);

    List<EvaluationJob> findBySubmissionIdIn(Collection<Long> submissionIds);

    Optional<EvaluationJob> findByIdempotencyKey(String idempotencyKey);

    List<EvaluationJob> findTop5ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            EvaluationJobStatus status,
            Instant now
    );
}
