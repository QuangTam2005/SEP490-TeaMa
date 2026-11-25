package com.asc.studycompanion.module.ai;

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
import com.asc.studycompanion.module.ai.provider.AiEvaluationProvider;
import com.asc.studycompanion.module.ai.provider.AiEvaluationResult;
import com.asc.studycompanion.module.ai.provider.SearchProvider;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.AIUsageLogRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.ai.service.EvaluationJobWorker;
import com.asc.studycompanion.module.ai.service.KnowledgeCacheService;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationJobWorkerTest {

    @Mock
    private EvaluationJobRepository evaluationJobRepository;
    @Mock
    private StudySubmissionRepository studySubmissionRepository;
    @Mock
    private StudyTaskRepository studyTaskRepository;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;
    @Mock
    private AIUsageLogRepository aiUsageLogRepository;
    @Mock
    private KnowledgeCacheService knowledgeCacheService;
    @Mock
    private SearchProvider searchProvider;
    @Mock
    private AiEvaluationProvider aiEvaluationProvider;

    private EvaluationJobWorker worker;
    private StudyTask task;
    private StudySubmission submission;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        worker = new EvaluationJobWorker(
                evaluationJobRepository,
                studySubmissionRepository,
                studyTaskRepository,
                aiEvaluationRepository,
                aiUsageLogRepository,
                knowledgeCacheService,
                searchProvider,
                aiEvaluationProvider,
                props
        );

        StudentProfile student = StudentProfile.builder().id(10L).grade(7).displayName("Khoi").build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).build();
        task = StudyTask.builder()
                .id(101L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.EVALUATING)
                .build();
        submission = StudySubmission.builder()
                .id(501L)
                .task(task)
                .version(1)
                .lessonTitle("Ước chung lớn nhất")
                .content("a".repeat(900))
                .charCount(900)
                .build();
    }

    @Test
    void cacheHitSkipsBraveSearchAndPersistsEvaluationAndUsageLog() {
        EvaluationJob job = EvaluationJob.builder()
                .id(901L)
                .submissionId(501L)
                .status(EvaluationJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .build();
        SearchSnippet cached = new SearchSnippet("KNTT", "https://loigiaihay.com", "SGK", Instant.now());

        when(evaluationJobRepository.findById(901L)).thenReturn(Optional.of(job));
        when(studySubmissionRepository.findById(501L)).thenReturn(Optional.of(submission));
        when(knowledgeCacheService.lookup(7, SubjectCode.MATH, "Ước chung lớn nhất"))
                .thenReturn(Optional.of(List.of(cached)));
        when(aiEvaluationProvider.evaluate(any())).thenReturn(new AiEvaluationResult(
                AccuracyLevel.CORRECT,
                CompletenessLevel.COMPLETE,
                OverallEvaluationStatus.GOOD,
                "Con làm rất tốt!",
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                400,
                100,
                "mimo-v2-flash",
                "{}"
        ));

        worker.processJob(901L);

        verify(searchProvider, never()).search(any());
        assertEquals(EvaluationJobStatus.SUCCEEDED, job.getStatus());
        assertEquals(StudyTaskStatus.EVALUATED, task.getStatus());

        ArgumentCaptor<AIEvaluation> evalCaptor = ArgumentCaptor.forClass(AIEvaluation.class);
        verify(aiEvaluationRepository).save(evalCaptor.capture());
        assertEquals(1, evalCaptor.getValue().getSources().size());

        ArgumentCaptor<AIUsageLog> logCaptor = ArgumentCaptor.forClass(AIUsageLog.class);
        verify(aiUsageLogRepository).save(logCaptor.capture());
        assertTrue(logCaptor.getValue().getCacheHit());
        assertEquals(0, logCaptor.getValue().getSearchCount());
        assertEquals("SUCCEEDED", logCaptor.getValue().getStatus());
    }

    @Test
    void retriesUpToThreeTimesThenPublishesFallbackNoticeAndMarksEvaluatedForGuardian() {
        EvaluationJob job = EvaluationJob.builder()
                .id(902L)
                .submissionId(501L)
                .status(EvaluationJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .build();

        when(evaluationJobRepository.findById(902L)).thenReturn(Optional.of(job));
        when(studySubmissionRepository.findById(501L)).thenReturn(Optional.of(submission));
        when(knowledgeCacheService.lookup(7, SubjectCode.MATH, "Ước chung lớn nhất")).thenReturn(Optional.empty());
        when(searchProvider.search(any())).thenReturn(List.of());
        when(aiEvaluationProvider.evaluate(any()))
                .thenThrow(new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Timeout"));

        // Attempt 1 -> PENDING backoff
        worker.processJob(902L);
        assertEquals(EvaluationJobStatus.PENDING, job.getStatus());
        assertEquals(1, job.getAttempts());
        assertEquals(StudyTaskStatus.EVALUATING, task.getStatus());

        // Attempt 2 -> PENDING backoff
        worker.processJob(902L);
        assertEquals(EvaluationJobStatus.PENDING, job.getStatus());
        assertEquals(2, job.getAttempts());

        // Attempt 3 -> stops retrying, stores fallback evaluation, marks task EVALUATED for Guardian
        when(aiEvaluationRepository.findBySubmissionId(501L)).thenReturn(Optional.empty());
        worker.processJob(902L);

        assertEquals(EvaluationJobStatus.FAILED, job.getStatus());
        assertEquals(3, job.getAttempts());
        assertEquals(StudyTaskStatus.EVALUATED, task.getStatus());

        ArgumentCaptor<AIEvaluation> fallbackCaptor = ArgumentCaptor.forClass(AIEvaluation.class);
        verify(aiEvaluationRepository).save(fallbackCaptor.capture());
        assertEquals(OverallEvaluationStatus.INSUFFICIENT_EVIDENCE, fallbackCaptor.getValue().getOverallStatus());
        assertEquals(EvaluationJobWorker.FALLBACK_GUARDIAN_FEEDBACK, fallbackCaptor.getValue().getFeedback());
    }
}
