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
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskDetailResponse;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        StudyTaskSummaryResponse summary = new StudyTaskSummaryResponse(
                task.getId(),
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                task.getRevisionCount(),
                StudyTask.MAX_REVISIONS,
                current != null ? current.lessonTitle() : null,
                (current != null && current.evaluation() != null) ? current.evaluation().overallStatus() : null,
                task.getGuardianAcknowledgedAt(),
                task.getGuardianNote()
        );

        return new StudyTaskDetailResponse(
                summary,
                task.getPlan().getTargetDate(),
                reviewCompleted,
                submissionResponses
        );
    }

    private boolean isReviewCompleted(StudyTask task, List<StudySubmission> taskSubmissions) {
        if (task.getTaskType() == StudyTaskType.REVIEW) {
            return !taskSubmissions.isEmpty();
        }
        return studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                        task.getPlan().getId(),
                        task.getSubject().getId(),
                        StudyTaskType.REVIEW
                )
                .map(reviewTask -> studySubmissionRepository.existsByTaskId(reviewTask.getId()))
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
