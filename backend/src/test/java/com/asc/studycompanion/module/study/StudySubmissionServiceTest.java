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
    void rejectsSubmissionOutOf850To2000Range() {
        StudyTask reviewTask = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.NOT_STARTED).revisionCount(0).build();
        when(studySubmissionRepository.findByIdempotencyKey("idem-short")).thenReturn(Optional.empty());
        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(reviewTask));
        when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(101L)).thenReturn(List.of());

        AppException ex = assertThrows(
                AppException.class,
                () -> submissionService.submitTask(1L, 101L, new SubmitStudyRequest("Bài ngắn", "a".repeat(849), "idem-short"), null)
        );
        assertEquals(ErrorCode.CONTENT_LENGTH_OUT_OF_RANGE, ex.getErrorCode());
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
}
