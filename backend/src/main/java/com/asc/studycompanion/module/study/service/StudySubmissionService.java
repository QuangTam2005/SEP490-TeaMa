package com.asc.studycompanion.module.study.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import com.asc.studycompanion.module.study.util.StudyTextUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudySubmissionService {

    private final StudyAccessGuard studyAccessGuard;
    private final StudyTaskRepository studyTaskRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final EvaluationJobRepository evaluationJobRepository;
    private final AIEvaluationRepository aiEvaluationRepository;
    private final StudyTaskService studyTaskService;

    @Autowired(required = false)
    private EntityManager entityManager;

    @Transactional
    public StudySubmissionResponse submitTask(
            Long studentUserId,
            Long taskId,
            SubmitStudyRequest request,
            String headerIdempotencyKey
    ) {
        String idempotencyKey = resolveIdempotencyKey(headerIdempotencyKey, request.idempotencyKey());

        Optional<StudySubmission> duplicateOpt = studySubmissionRepository.findByIdempotencyKey(idempotencyKey);
        if (duplicateOpt.isPresent()) {
            StudySubmission existing = duplicateOpt.get();
            studyAccessGuard.assertStudentOwner(studentUserId, existing.getTask().getPlan().getStudent().getId());
            AIEvaluation evaluation = aiEvaluationRepository.findBySubmissionId(existing.getId()).orElse(null);
            String lastError = evaluationJobRepository.findBySubmissionId(existing.getId())
                    .map(EvaluationJob::getLastError)
                    .orElse(null);
            return studyTaskService.toSubmissionResponse(existing, evaluation, lastError);
        }

        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertStudentOwner(studentUserId, task.getPlan().getStudent().getId());
        if (entityManager != null && task.getId() != null) {
            entityManager.find(StudyTask.class, task.getId(), LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(task);
        }

        if (task.getStatus() == StudyTaskStatus.SKIPPED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Nhiệm vụ này đã được phụ huynh đánh dấu bỏ qua");
        }
        if (task.getAssignmentMode() == TaskAssignmentMode.CUSTOM_DRIVE) {
            throw new AppException(
                    ErrorCode.VALIDATION_FAILED,
                    "Nhiệm vụ này đã được phụ huynh thay thế bằng bài nộp qua Google Drive"
            );
        }

        if (task.getTaskType() == StudyTaskType.PREVIEW) {
            ensureReviewSubmittedFirst(task);
        }

        if (task.getStatus() == StudyTaskStatus.EVALUATING || task.getStatus() == StudyTaskStatus.SUBMITTED) {
            throw new AppException(ErrorCode.SUBMISSION_EVALUATING_LOCKED);
        }

        List<StudySubmission> existingSubs = studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(taskId);
        if (existingSubs.size() >= StudySubmission.MAX_VERSIONS
                || task.getRevisionCount() >= StudyTask.MAX_REVISIONS) {
            throw new AppException(ErrorCode.MAX_REVISIONS_EXCEEDED);
        }

        String normalizedContent = StudyTextUtils.normalize(request.content());
        int charCount = normalizedContent.length();
        if (charCount < StudyTextUtils.MIN_SUBMISSION_CHARS || charCount > StudyTextUtils.MAX_SUBMISSION_CHARS) {
            throw new AppException(ErrorCode.CONTENT_LENGTH_OUT_OF_RANGE);
        }

        if (!existingSubs.isEmpty()) {
            for (StudySubmission old : existingSubs) {
                old.setIsCurrent(Boolean.FALSE);
            }
            studySubmissionRepository.saveAll(existingSubs);
        }

        int nextVersion = existingSubs.size() + 1;
        StudySubmission createdSubmission = StudySubmission.builder()
                .task(task)
                .version(nextVersion)
                .isCurrent(Boolean.TRUE)
                .lessonTitle(request.lessonTitle())
                .content(normalizedContent)
                .charCount(charCount)
                .idempotencyKey(idempotencyKey)
                .build();

        StudySubmission savedSubmission = studySubmissionRepository.saveAndFlush(createdSubmission);

        task.setStatus(StudyTaskStatus.EVALUATING);
        task.setRevisionCount(Math.max(0, nextVersion - 1));
        if (task.getSkipRequestStatus() == SkipRequestStatus.PENDING) {
            task.setSkipRequestStatus(null);
            task.setSkipRequestReason(null);
            task.setSkipRequestedAt(null);
            task.setSkipReviewedAt(null);
            task.setSkipReviewedByUserId(null);
        }
        studyTaskRepository.save(task);

        if (task.getTaskType() == StudyTaskType.REVIEW) {
            unlockPreviewSiblingIfBlocked(task);
        }

        EvaluationJob job = EvaluationJob.builder()
                .submissionId(savedSubmission.getId())
                .idempotencyKey(idempotencyKey)
                .status(EvaluationJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(EvaluationJob.DEFAULT_MAX_ATTEMPTS)
                .nextRetryAt(Instant.now())
                .build();
        evaluationJobRepository.save(job);

        return studyTaskService.toSubmissionResponse(savedSubmission, null, null);
    }

    @Transactional
    public StudySubmissionResponse retryEvaluation(Long studentUserId, Long submissionId) {
        StudySubmission submission = studySubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        StudyTask task = submission.getTask();
        studyAccessGuard.assertStudentOwner(studentUserId, task.getPlan().getStudent().getId());

        EvaluationJob job = evaluationJobRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.EVALUATION_JOB_NOT_FOUND));

        if (task.getStatus() == StudyTaskStatus.EVALUATED
                || (job.getStatus() != EvaluationJobStatus.FAILED
                && task.getStatus() != StudyTaskStatus.EVALUATION_FAILED)) {
            throw new AppException(ErrorCode.AI_RETRY_NOT_ALLOWED);
        }

        job.setStatus(EvaluationJobStatus.PENDING);
        job.setAttempts(0);
        job.setLastError(null);
        job.setNextRetryAt(Instant.now());
        evaluationJobRepository.save(job);

        task.setStatus(StudyTaskStatus.EVALUATING);
        studyTaskRepository.save(task);

        return studyTaskService.toSubmissionResponse(submission, null, null);
    }

    private void ensureReviewSubmittedFirst(StudyTask previewTask) {
        boolean reviewExists = studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                        previewTask.getPlan().getId(),
                        previewTask.getSubject().getId(),
                        StudyTaskType.REVIEW
                )
                .map(review -> review.getStatus() == StudyTaskStatus.SKIPPED
                        || review.getCustomSubmittedAt() != null
                        || studySubmissionRepository.existsByTaskId(review.getId()))
                .orElse(false);

        if (!reviewExists) {
            throw new AppException(ErrorCode.REVIEW_REQUIRED_BEFORE_PREVIEW);
        }
    }

    private void unlockPreviewSiblingIfBlocked(StudyTask reviewTask) {
        studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                        reviewTask.getPlan().getId(),
                        reviewTask.getSubject().getId(),
                        StudyTaskType.PREVIEW
                )
                .ifPresent(preview -> {
                    if (preview.getStatus() == StudyTaskStatus.BLOCKED) {
                        preview.setStatus(StudyTaskStatus.NOT_STARTED);
                        studyTaskRepository.save(preview);
                    }
                });
    }

    private String resolveIdempotencyKey(String headerKey, String bodyKey) {
        String resolved = (headerKey != null && !headerKey.isBlank()) ? headerKey.trim() : bodyKey;
        if (resolved == null || resolved.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Thiếu mã chống trùng Idempotency-Key");
        }
        String trimmed = resolved.trim();
        if (trimmed.length() > 64) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã chống trùng Idempotency-Key tối đa 64 ký tự");
        }
        return trimmed;
    }
}
