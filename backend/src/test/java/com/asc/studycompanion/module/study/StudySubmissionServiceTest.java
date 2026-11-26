package com.asc.studycompanion.module.study;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.dto.SubmitStudyRequest;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySubmissionServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private StudyTaskRepository studyTaskRepository;
    @Mock
    private StudySubmissionRepository studySubmissionRepository;
    @Mock
    private EvaluationJobRepository evaluationJobRepository;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;

    private StudyTaskService studyTaskService;
    private StudySubmissionService submissionService;

    private StudentProfile student;
    private Subject math;
    private StudyPlan plan;

    @BeforeEach
    void setUp() {
        studyTaskService = new StudyTaskService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository
        );
        submissionService = new StudySubmissionService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                evaluationJobRepository,
                aiEvaluationRepository,
                studyTaskService
        );

        student = StudentProfile.builder().id(10L).displayName("Khoi").grade(7).build();
        math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        plan = StudyPlan.builder().id(20L).student(student).build();
    }

    @Test
    void submitReviewCreatesSubmissionAndJobAndUnlocksPreview() {
        StudyTask reviewTask = StudyTask.builder()
                .id(101L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .revisionCount(0)
                .build();
        StudyTask previewTask = StudyTask.builder()
                .id(102L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.PREVIEW)
                .status(StudyTaskStatus.BLOCKED)
                .revisionCount(0)
                .build();

        String content = "a".repeat(900);
        SubmitStudyRequest request = new SubmitStudyRequest("Ôn bài ƯCLN", content, "idem-1");

        when(studySubmissionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(reviewTask));
        when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(101L)).thenReturn(List.of());
        when(studySubmissionRepository.saveAndFlush(any(StudySubmission.class))).thenAnswer(inv -> {
            StudySubmission s = inv.getArgument(0);
            s.setId(501L);
            s.setSubmittedAt(Instant.now());
            return s;
        });
        when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(20L, 1L, StudyTaskType.PREVIEW))
                .thenReturn(Optional.of(previewTask));

        StudySubmissionResponse response = submissionService.submitTask(1L, 101L, request, null);

        assertEquals(501L, response.id());
        assertEquals(1, response.version());
        assertEquals(900, response.charCount());
        assertEquals(StudyTaskStatus.EVALUATING, reviewTask.getStatus());
        assertEquals(StudyTaskStatus.NOT_STARTED, previewTask.getStatus());

        ArgumentCaptor<EvaluationJob> jobCaptor = ArgumentCaptor.forClass(EvaluationJob.class);
        verify(evaluationJobRepository).save(jobCaptor.capture());
        assertEquals(EvaluationJobStatus.PENDING, jobCaptor.getValue().getStatus());
        assertEquals(501L, jobCaptor.getValue().getSubmissionId());
        assertEquals(3, jobCaptor.getValue().getMaxAttempts());
    }

    @Test
    void blocksPreviewBeforeReviewIsSubmitted() {
        StudyTask reviewTask = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).build();
        StudyTask previewTask = StudyTask.builder().id(102L).plan(plan).subject(math).taskType(StudyTaskType.PREVIEW).status(StudyTaskStatus.BLOCKED).build();

        when(studySubmissionRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(102L)).thenReturn(Optional.of(previewTask));
        when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(20L, 1L, StudyTaskType.REVIEW))
                .thenReturn(Optional.of(reviewTask));
        when(studySubmissionRepository.existsByTaskId(101L)).thenReturn(false);

        AppException ex = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 102L, new SubmitStudyRequest("Bài mới", "a".repeat(900), "idem-2"), null)
        );
        assertEquals(ErrorCode.REVIEW_REQUIRED_BEFORE_PREVIEW, ex.getErrorCode());
    }

    @Test
    void rejectsSubmissionOutOf400To3000RangeAndBlocksSkippedTasks() {
        StudyTask reviewTask = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.NOT_STARTED).revisionCount(0).build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-short")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(reviewTask));
        when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(101L)).thenReturn(List.of());

        AppException ex = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài ngắn", "a".repeat(399), "idem-short"), null)
        );
        assertEquals(ErrorCode.CONTENT_LENGTH_OUT_OF_RANGE, ex.getErrorCode());

        StudyTask skippedTask = StudyTask.builder().id(109L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.SKIPPED).revisionCount(0).build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-skip")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(109L)).thenReturn(Optional.of(skippedTask));
        AppException skippedEx = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 109L, new SubmitStudyRequest("Bài bỏ qua", "a".repeat(450), "idem-skip"), null)
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, skippedEx.getErrorCode());
    }

    @Test
    void enforcesMax3VersionsV1ToV3AndEvaluatingLockAndIdempotency() {
        StudyTask evaluatingTask = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATING).revisionCount(0).build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-lock")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(evaluatingTask));

        AppException lockEx = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài", "a".repeat(900), "idem-lock"), null)
        );
        assertEquals(ErrorCode.SUBMISSION_EVALUATING_LOCKED, lockEx.getErrorCode());

        StudyTask maxedTask = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).revisionCount(2).build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-max")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(maxedTask));
        when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(101L)).thenReturn(List.of(
                StudySubmission.builder().id(3L).version(3).isCurrent(true).build(),
                StudySubmission.builder().id(2L).version(2).isCurrent(false).build(),
                StudySubmission.builder().id(1L).version(1).isCurrent(false).build()
        ));

        AppException maxEx = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài 4", "a".repeat(900), "idem-max"), null)
        );
        assertEquals(ErrorCode.MAX_REVISIONS_EXCEEDED, maxEx.getErrorCode());

        StudySubmission existingSub = StudySubmission.builder()
                .id(501L)
                .task(maxedTask)
                .version(1)
                .isCurrent(false)
                .lessonTitle("Bài 1")
                .content("a".repeat(900))
                .charCount(900)
                .idempotencyKey("idem-dup")
                .submittedAt(Instant.now())
                .build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existingSub));

        StudySubmissionResponse deduped = submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài 1", "a".repeat(900), null), "idem-dup");
        assertEquals(501L, deduped.id());
        assertFalse(deduped.isCurrent());
    }

    @Test
    void retryEvaluationResetsFailedJobAndRejectsSucceededJob() {
        StudyTask task = StudyTask.builder()
                .id(101L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.EVALUATION_FAILED)
                .build();
        StudySubmission submission = StudySubmission.builder()
                .id(601L)
                .task(task)
                .version(1)
                .isCurrent(true)
                .lessonTitle("Bài ôn")
                .content("a".repeat(900))
                .charCount(900)
                .submittedAt(Instant.now())
                .build();
        EvaluationJob failedJob = EvaluationJob.builder()
                .id(801L)
                .submissionId(601L)
                .status(EvaluationJobStatus.FAILED)
                .attempts(3)
                .lastError("Timeout")
                .build();

        when(studySubmissionRepository.findById(601L)).thenReturn(Optional.of(submission));
        when(evaluationJobRepository.findBySubmissionId(601L)).thenReturn(Optional.of(failedJob));

        StudySubmissionResponse retried = submissionService.retryEvaluation(1L, 601L);
        assertEquals(601L, retried.id());
        assertEquals(EvaluationJobStatus.PENDING, failedJob.getStatus());
        assertEquals(0, failedJob.getAttempts());
        assertEquals(StudyTaskStatus.EVALUATING, task.getStatus());

        task.setStatus(StudyTaskStatus.EVALUATED);
        failedJob.setStatus(EvaluationJobStatus.SUCCEEDED);
        AppException rejectEx = assertThrows(
                AppException.class,
                () -> submissionService.retryEvaluation(1L, 601L)
        );
        assertEquals(ErrorCode.AI_RETRY_NOT_ALLOWED, rejectEx.getErrorCode());

        // Fallback state (task EVALUATED while job FAILED) must also reject retry to protect uq_ai_evaluations_submission
        failedJob.setStatus(EvaluationJobStatus.FAILED);
        AppException fallbackReject = assertThrows(
                AppException.class,
                () -> submissionService.retryEvaluation(1L, 601L)
        );
        assertEquals(ErrorCode.AI_RETRY_NOT_ALLOWED, fallbackReject.getErrorCode());

        // Oversized Idempotency-Key header > 64 chars throws VAL_001 instead of DB truncation 500
        AppException keyTooLong = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài", "a".repeat(450), null), "k".repeat(65))
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, keyTooLong.getErrorCode());

        StudyTask customDriveTask = StudyTask.builder()
                .id(105L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .assignmentMode(TaskAssignmentMode.CUSTOM_DRIVE)
                .customTaskTitle("Làm phiếu bài tập")
                .customDriveFolderUrl("https://drive.google.com/drive/folders/abc")
                .build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-custom")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(105L)).thenReturn(Optional.of(customDriveTask));
        AppException customBlocked = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 105L, new SubmitStudyRequest("Bài", "a".repeat(450), "idem-custom"), null)
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, customBlocked.getErrorCode());
    }
}
