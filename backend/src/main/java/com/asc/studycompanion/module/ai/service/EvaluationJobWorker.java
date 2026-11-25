package com.asc.studycompanion.module.ai.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.AIUsageLog;
import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.entity.SourceReference;
import com.asc.studycompanion.module.ai.provider.AiEvaluationInput;
import com.asc.studycompanion.module.ai.provider.AiEvaluationProvider;
import com.asc.studycompanion.module.ai.provider.AiEvaluationResult;
import com.asc.studycompanion.module.ai.provider.SearchProvider;
import com.asc.studycompanion.module.ai.provider.SearchQuery;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.AIUsageLogRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Background worker polling due EvaluationJobs, calling KnowledgeCache -> BraveSearch -> CkeyMimo.
 * Retries transient errors up to maxAttempts (3) with exponential backoff; after 3 failures, writes
 * a clear fallback notice for the Guardian to inspect the student's original text directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationJobWorker {

    public static final String FALLBACK_GUARDIAN_FEEDBACK =
            "Hệ thống AI đang gặp lỗi sau 3 lần thử đối chiếu tự động. Phụ huynh vui lòng tự kiểm tra trực tiếp bài làm của con nhé.";

    private static final BigDecimal INPUT_TOKEN_USD_RATE = new BigDecimal("0.0000003");
    private static final BigDecimal OUTPUT_TOKEN_USD_RATE = new BigDecimal("0.0000012");

    private final EvaluationJobRepository evaluationJobRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final StudyTaskRepository studyTaskRepository;
    private final AIEvaluationRepository aiEvaluationRepository;
    private final AIUsageLogRepository aiUsageLogRepository;
    private final KnowledgeCacheService knowledgeCacheService;
    private final SearchProvider searchProvider;
    private final AiEvaluationProvider aiEvaluationProvider;
    private final AiProperties aiProperties;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelayString = "${app.ai.worker-poll-ms:3000}")
    public void pollAndExecuteDueJobs() {
        List<EvaluationJob> dueJobs = evaluationJobRepository
                .findTop5ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        EvaluationJobStatus.PENDING,
                        Instant.now()
                );
        for (EvaluationJob job : dueJobs) {
            if (transactionManager != null) {
                TransactionTemplate perJobTx = new TransactionTemplate(transactionManager);
                perJobTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    perJobTx.executeWithoutResult(status -> processJob(job.getId()));
                } catch (RuntimeException ex) {
                    log.error("Isolated EvaluationJob {} failed unexpectedly: {}", job.getId(), ex.getMessage(), ex);
                }
            } else {
                processJob(job.getId());
            }
        }
    }

    @Transactional
    public void processJob(Long jobId) {
        EvaluationJob job = evaluationJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != EvaluationJobStatus.PENDING) {
            return;
        }

        long startedNanos = System.nanoTime();
        int currentAttempt = job.getAttempts() + 1;
        job.setStatus(EvaluationJobStatus.RUNNING);
        job.setAttempts(currentAttempt);
        evaluationJobRepository.save(job);

        StudySubmission submission = studySubmissionRepository.findById(job.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
        StudyTask task = submission.getTask();
        int grade = task.getPlan().getStudent().getGrade();

        int searchCount = 0;
        boolean cacheHit = false;

        try {
            Optional<List<SearchSnippet>> cachedSources = knowledgeCacheService.lookup(
                    grade,
                    task.getSubject().getCode(),
                    submission.getLessonTitle()
            );

            List<SearchSnippet> snippets;
            if (cachedSources.isPresent()) {
                snippets = cachedSources.get();
                cacheHit = true;
            } else {
                snippets = searchProvider.search(new SearchQuery(
                        grade,
                        task.getSubject().getCode(),
                        task.getSubject().getName(),
                        submission.getLessonTitle(),
                        submission.getContent()
                ));
                searchCount = (searchProvider instanceof com.asc.studycompanion.module.ai.provider.BraveSearchAdapter braveAdapter)
                        ? braveAdapter.consumeLastRequestCount()
                        : 1;
                knowledgeCacheService.store(
                        grade,
                        task.getSubject().getCode(),
                        submission.getLessonTitle(),
                        snippets,
                        true
                );
            }

            AiEvaluationResult aiResult = aiEvaluationProvider.evaluate(new AiEvaluationInput(
                    grade,
                    task.getSubject().getCode(),
                    task.getSubject().getName(),
                    task.getTaskType(),
                    submission.getLessonTitle(),
                    submission.getContent(),
                    snippets
            ));

            saveSucceededEvaluation(submission, task, job, aiResult, snippets);
            logUsage(
                    job.getId(),
                    aiResult.modelUsed(),
                    aiResult.inputTokens(),
                    aiResult.outputTokens(),
                    searchCount,
                    cacheHit,
                    elapsedMs(startedNanos),
                    "SUCCEEDED",
                    null
            );
        } catch (AppException ex) {
            handleJobFailure(job, submission, task, currentAttempt, searchCount, cacheHit, elapsedMs(startedNanos), ex.getErrorCode().getCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            handleJobFailure(job, submission, task, currentAttempt, searchCount, cacheHit, elapsedMs(startedNanos), ErrorCode.AI_PROVIDER_UNAVAILABLE.getCode(), ex.getMessage());
        }
    }

    private void saveSucceededEvaluation(
            StudySubmission submission,
            StudyTask task,
            EvaluationJob job,
            AiEvaluationResult aiResult,
            List<SearchSnippet> snippets
    ) {
        AIEvaluation evaluation = aiEvaluationRepository.findBySubmissionId(submission.getId())
                .orElseGet(() -> AIEvaluation.builder().submissionId(submission.getId()).build());
        evaluation.setAccuracy(aiResult.accuracy());
        evaluation.setCompleteness(aiResult.completeness());
        evaluation.setOverallStatus(aiResult.overallStatus());
        evaluation.setFeedback(aiResult.feedback());
        evaluation.setIncorrectPoints(aiResult.incorrectPoints());
        evaluation.setMissingPoints(aiResult.missingPoints());
        evaluation.setConfidence(aiResult.confidence());
        evaluation.setRawSanitizedResponse(aiResult.rawSanitizedJson());
        evaluation.setEvaluatedAt(Instant.now());
        evaluation.getSources().clear();

        if (snippets != null) {
            for (SearchSnippet snippet : snippets) {
                evaluation.getSources().add(SourceReference.builder()
                        .evaluation(evaluation)
                        .title(snippet.title())
                        .url(snippet.url())
                        .snippet(snippet.snippet())
                        .retrievedAt(snippet.retrievedAt() != null ? snippet.retrievedAt() : Instant.now())
                        .build());
            }
        }

        aiEvaluationRepository.save(evaluation);

        job.setStatus(EvaluationJobStatus.SUCCEEDED);
        job.setLastError(null);
        evaluationJobRepository.save(job);

        task.setStatus(StudyTaskStatus.EVALUATED);
        studyTaskRepository.save(task);
    }

    private void handleJobFailure(
            EvaluationJob job,
            StudySubmission submission,
            StudyTask task,
            int currentAttempt,
            int searchCount,
            boolean cacheHit,
            int latencyMs,
            String errorCode,
            String errorMessage
    ) {
        log.warn("EvaluationJob {} attempt {}/{} failed [{}]: {}",
                job.getId(), currentAttempt, job.getMaxAttempts(), errorCode, errorMessage);

        logUsage(
                job.getId(),
                aiProperties.getCkey().getModel(),
                0,
                0,
                searchCount,
                cacheHit,
                latencyMs,
                "FAILED",
                errorCode
        );

        String safeError = errorMessage != null ? errorMessage : "Lỗi kết nối AI";
        job.setLastError(safeError);

        if (currentAttempt < job.getMaxAttempts()) {
            long baseDelaySeconds = 30L * (1L << Math.min(currentAttempt - 1, 4));
            long jitterSeconds = ThreadLocalRandom.current().nextLong(1, 6);
            job.setStatus(EvaluationJobStatus.PENDING);
            job.setNextRetryAt(Instant.now().plusSeconds(baseDelaySeconds + jitterSeconds));
            evaluationJobRepository.save(job);
            return;
        }

        // After 3 automatic retries fail, stop retrying and pass fallback feedback to Guardian as reviewed
        job.setStatus(EvaluationJobStatus.FAILED);
        evaluationJobRepository.save(job);

        if (aiEvaluationRepository.findBySubmissionId(submission.getId()).isEmpty()) {
            AIEvaluation fallback = AIEvaluation.builder()
                    .submissionId(submission.getId())
                    .accuracy(AccuracyLevel.UNVERIFIABLE)
                    .completeness(CompletenessLevel.UNDETERMINED)
                    .overallStatus(OverallEvaluationStatus.INSUFFICIENT_EVIDENCE)
                    .feedback(FALLBACK_GUARDIAN_FEEDBACK)
                    .incorrectPoints(List.of())
                    .missingPoints(List.of())
                    .confidence(ConfidenceLevel.LOW)
                    .evaluatedAt(Instant.now())
                    .build();
            aiEvaluationRepository.save(fallback);
        }

        task.setStatus(StudyTaskStatus.EVALUATED);
        studyTaskRepository.save(task);
    }

    private void logUsage(
            Long jobId,
            String model,
            int inputTokens,
            int outputTokens,
            int searchCount,
            boolean cacheHit,
            int latencyMs,
            String status,
            String errorCode
    ) {
        BigDecimal estimatedCost = INPUT_TOKEN_USD_RATE.multiply(BigDecimal.valueOf(inputTokens))
                .add(OUTPUT_TOKEN_USD_RATE.multiply(BigDecimal.valueOf(outputTokens)))
                .setScale(6, RoundingMode.HALF_UP);

        aiUsageLogRepository.save(AIUsageLog.builder()
                .jobId(jobId)
                .provider("CKEY_MIMO")
                .model(model != null ? model : "mimo-v2-flash")
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .searchCount(searchCount)
                .cacheHit(cacheHit)
                .estimatedCostUsd(estimatedCost)
                .latencyMs(latencyMs)
                .status(status)
                .errorCode(errorCode)
                .build());
    }

    private static int elapsedMs(long startedNanos) {
        return (int) Math.max(1L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }
}
