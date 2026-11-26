package com.asc.studycompanion.module.study.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.dto.AIEvaluationResponse;
import com.asc.studycompanion.module.ai.dto.SourceReferenceResponse;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.StudentCustomSubmitRequest;
import com.asc.studycompanion.module.study.dto.StudentSkipRequestDto;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyTaskService {

    private final StudyAccessGuard studyAccessGuard;
    private final StudyTaskRepository studyTaskRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final AIEvaluationRepository aiEvaluationRepository;
    private final EvaluationJobRepository evaluationJobRepository;

    @Transactional(readOnly = true)
    public StudyTaskDetailResponse getTaskDetail(Long studentUserId, Long taskId) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));

        studyAccessGuard.assertStudentOwner(studentUserId, task.getPlan().getStudent().getId());

        List<StudySubmission> submissions = studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(taskId);
        List<Long> subIds = submissions.stream().map(StudySubmission::getId).toList();
        Map<Long, AIEvaluation> evaluationsBySubId = subIds.isEmpty()
                ? Map.of()
                : aiEvaluationRepository.findBySubmissionIdIn(subIds)
                        .stream()
                        .collect(Collectors.toMap(AIEvaluation::getSubmissionId, Function.identity()));
        Map<Long, String> errorsBySubId = subIds.isEmpty()
                ? Map.of()
                : evaluationJobRepository.findBySubmissionIdIn(subIds)
                        .stream()
                        .filter(job -> job.getLastError() != null)
                        .collect(Collectors.toMap(EvaluationJob::getSubmissionId, EvaluationJob::getLastError));

        List<StudySubmissionResponse> submissionResponses = submissions.stream()
                .map(sub -> toSubmissionResponse(
                        sub,
                        evaluationsBySubId.get(sub.getId()),
                        errorsBySubId.get(sub.getId())
                ))
                .toList();

        boolean reviewCompleted = isReviewCompleted(task, submissions);
        StudySubmissionResponse current = submissionResponses.stream()
                .filter(StudySubmissionResponse::isCurrent)
                .findFirst()
                .orElse(submissionResponses.isEmpty() ? null : submissionResponses.get(0));

        StudyTaskSummaryResponse summary = StudyTaskSummaryResponse.from(
                task,
                current != null ? current.lessonTitle() : null,
                (current != null && current.evaluation() != null) ? current.evaluation().overallStatus() : null
        );

        return new StudyTaskDetailResponse(
                summary,
                task.getPlan().getTargetDate(),
                reviewCompleted,
                submissionResponses
        );
    }

    @Transactional
    public StudyTaskSummaryResponse requestSkip(
            Long studentUserId,
            Long taskId,
            StudentSkipRequestDto request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        Long studentId = task.getPlan().getStudent().getId();
        studyAccessGuard.assertStudentOwner(studentUserId, studentId);
        studyAccessGuard.assertStudentHasActiveGuardian(studentId);

        if (task.getStatus() == StudyTaskStatus.SKIPPED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Bài học này đã được phụ huynh đánh dấu bỏ qua");
        }

        boolean hasSubmission = studySubmissionRepository.existsByTaskId(taskId)
                || task.getCustomSubmittedAt() != null
                || task.getStatus() == StudyTaskStatus.SUBMITTED
                || task.getStatus() == StudyTaskStatus.EVALUATING
                || task.getStatus() == StudyTaskStatus.EVALUATED
                || task.getStatus() == StudyTaskStatus.EVALUATION_FAILED;
        if (hasSubmission) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Không thể xin bỏ qua bài học đã nộp bài");
        }

        String normalizedReason = request != null && request.reason() != null ? request.reason().trim() : "";
        if (normalizedReason.length() < 5 || normalizedReason.length() > 300) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Lý do xin bỏ qua phải từ 5 đến 300 ký tự");
        }

        task.setSkipRequestStatus(SkipRequestStatus.PENDING);
        task.setSkipRequestReason(normalizedReason);
        task.setSkipRequestedAt(Instant.now());
        task.setSkipReviewedAt(null);
        task.setSkipReviewedByUserId(null);
        studyTaskRepository.save(task);

        return StudyTaskSummaryResponse.from(task, null, null);
    }

    @Transactional
    public StudyTaskSummaryResponse cancelSkipRequest(Long studentUserId, Long taskId) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertStudentOwner(studentUserId, task.getPlan().getStudent().getId());

        if (task.getSkipRequestStatus() != SkipRequestStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ có thể hủy yêu cầu xin bỏ qua đang chờ phụ huynh duyệt");
        }

        task.setSkipRequestStatus(null);
        task.setSkipRequestReason(null);
        task.setSkipRequestedAt(null);
        task.setSkipReviewedAt(null);
        task.setSkipReviewedByUserId(null);
        studyTaskRepository.save(task);

        return StudyTaskSummaryResponse.from(task, null, null);
    }

    @Transactional
    public StudyTaskSummaryResponse submitCustomTask(
            Long studentUserId,
            Long taskId,
            StudentCustomSubmitRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertStudentOwner(studentUserId, task.getPlan().getStudent().getId());

        if (task.getAssignmentMode() != TaskAssignmentMode.CUSTOM_DRIVE) {
            throw new AppException(
                    ErrorCode.VALIDATION_FAILED,
                    "Nhiệm vụ này đang ở chế độ nộp bài AI thông thường"
            );
        }
        if (task.getStatus() == StudyTaskStatus.SKIPPED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Nhiệm vụ này đã được phụ huynh đánh dấu bỏ qua");
        }
        if (task.getStatus() == StudyTaskStatus.EVALUATED || task.getStatus() == StudyTaskStatus.SUBMITTED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Con đã xác nhận nộp bài này lên Drive rồi");
        }
        if (task.getTaskType() == StudyTaskType.PREVIEW && !isReviewCompleted(task, List.of())) {
            throw new AppException(ErrorCode.REVIEW_REQUIRED_BEFORE_PREVIEW);
        }

        String note = request != null && request.submissionNote() != null ? request.submissionNote().trim() : "";
        if (note.length() < 5 || note.length() > 300) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ghi chú nộp bài phải từ 5 đến 300 ký tự");
        }

        String validatedFileUrl = normalizeOptionalHttpUrl(request != null ? request.driveFileUrl() : null);

        task.setCustomSubmissionNote(note);
        task.setCustomSubmissionUrl(validatedFileUrl);
        task.setCustomSubmittedAt(Instant.now());
        task.setStatus(StudyTaskStatus.SUBMITTED);
        if (task.getSkipRequestStatus() == SkipRequestStatus.PENDING) {
            task.setSkipRequestStatus(null);
            task.setSkipRequestReason(null);
            task.setSkipRequestedAt(null);
            task.setSkipReviewedAt(null);
            task.setSkipReviewedByUserId(null);
        }
        studyTaskRepository.save(task);

        if (task.getTaskType() == StudyTaskType.REVIEW) {
            studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                            task.getPlan().getId(),
                            task.getSubject().getId(),
                            StudyTaskType.PREVIEW
                    )
                    .ifPresent(preview -> {
                        if (preview.getStatus() == StudyTaskStatus.BLOCKED) {
                            preview.setStatus(StudyTaskStatus.NOT_STARTED);
                            studyTaskRepository.save(preview);
                        }
                    });
        }

        return StudyTaskSummaryResponse.from(task, null, null);
    }

    private static String normalizeOptionalHttpUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > 500) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Link file Google Drive tối đa 500 ký tự");
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !"https".equalsIgnoreCase(scheme)
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Link file Google Drive không hợp lệ (cần bắt đầu bằng https://)");
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Link file Google Drive không hợp lệ");
        }
    }

    private boolean isReviewCompleted(StudyTask task, List<StudySubmission> taskSubmissions) {
        if (task.getTaskType() == StudyTaskType.REVIEW) {
            return task.getCustomSubmittedAt() != null || !taskSubmissions.isEmpty();
        }
        return studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                        task.getPlan().getId(),
                        task.getSubject().getId(),
                        StudyTaskType.REVIEW
                )
                .map(reviewTask -> reviewTask.getStatus() == StudyTaskStatus.SKIPPED
                        || reviewTask.getCustomSubmittedAt() != null
                        || studySubmissionRepository.existsByTaskId(reviewTask.getId()))
                .orElse(false);
    }

    public StudySubmissionResponse toSubmissionResponse(
            StudySubmission submission,
            AIEvaluation evaluation,
            String lastError
    ) {
        return new StudySubmissionResponse(
                submission.getId(),
                submission.getTask().getId(),
                submission.getVersion(),
                Boolean.TRUE.equals(submission.getIsCurrent()),
                submission.getLessonTitle(),
                submission.getContent(),
                submission.getCharCount(),
                submission.getSubmittedAt(),
                evaluation != null ? toEvaluationResponse(evaluation) : null,
                lastError
        );
    }

    private AIEvaluationResponse toEvaluationResponse(AIEvaluation eval) {
        List<SourceReferenceResponse> sources = eval.getSources().stream()
                .map(src -> new SourceReferenceResponse(
                        src.getId(),
                        src.getTitle(),
                        src.getUrl(),
                        src.getSnippet(),
                        src.getRetrievedAt()
                ))
                .toList();

        return new AIEvaluationResponse(
                eval.getId(),
                eval.getSubmissionId(),
                eval.getAccuracy(),
                eval.getCompleteness(),
                eval.getOverallStatus(),
                eval.getFeedback(),
                eval.getIncorrectPoints(),
                eval.getMissingPoints(),
                eval.getConfidence(),
                sources,
                eval.getEvaluatedAt()
        );
    }
}
