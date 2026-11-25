package com.asc.studycompanion.module.guardian;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.entity.SourceReference;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.service.GuardianService;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private StudySubmissionRepository studySubmissionRepository;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;
    @Mock
    private EvaluationJobRepository evaluationJobRepository;
    @Mock
    private StudyTaskRepository studyTaskRepository;

    private GuardianService guardianService;

    @BeforeEach
    void setUp() {
        StudyTaskService studyTaskService = new StudyTaskService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository
        );
        guardianService = new GuardianService(
                studyAccessGuard,
                studyPlanRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository,
                studyTaskService,
                studyTaskRepository
        );
    }

    @Test
    void dashboardFiltersByDateAndSubjectAndSummarizesProgress() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(8).timezone("Asia/Ho_Chi_Minh").build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        Subject lit = Subject.builder().id(2L).code(SubjectCode.LIT).name("Ngữ văn").build();
        LocalDate date = LocalDate.of(2026, 11, 30);

        StudyTask mathReview = StudyTask.builder().id(101L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).revisionCount(1).build();
        StudyTask litReview = StudyTask.builder().id(102L).subject(lit).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.NOT_STARTED).revisionCount(0).build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(date).tasks(List.of(mathReview, litReview)).build();

        StudySubmission sub = StudySubmission.builder().id(501L).task(mathReview).version(2).isCurrent(true).lessonTitle("Hằng đẳng thức").content("a".repeat(900)).charCount(900).submittedAt(Instant.now()).build();
        AIEvaluation eval = AIEvaluation.builder().id(701L).submissionId(501L).accuracy(AccuracyLevel.CORRECT).completeness(CompletenessLevel.COMPLETE).overallStatus(OverallEvaluationStatus.GOOD).feedback("Rất tốt").evaluatedAt(Instant.now()).build();

        when(studyAccessGuard.requireLinkedStudentForGuardian(2L)).thenReturn(student);
        when(studyPlanRepository.findByStudentIdAndTargetDate(10L, date)).thenReturn(Optional.of(plan));
        when(studySubmissionRepository.findFirstByTaskIdAndIsCurrentTrue(101L)).thenReturn(Optional.of(sub));
        when(aiEvaluationRepository.findBySubmissionId(501L)).thenReturn(Optional.of(eval));

        GuardianDashboardResponse filtered = guardianService.getDashboard(2L, date, "MATH");

        assertEquals("Khoi", filtered.linkedStudentName());
        assertEquals(8, filtered.grade());
        assertEquals("MATH", filtered.selectedSubject());
        assertEquals(1, filtered.totalTasks());
        assertEquals(1, filtered.completedTasks());
        assertEquals(501L, filtered.items().get(0).currentSubmissionId());
        assertEquals("Rất tốt", filtered.items().get(0).evaluation().feedback());
    }

    @Test
    void submissionDetailReturnsVersionsAndSourcesAndRejectsUnlinkedGuardian() {
        StudentProfile otherStudent = StudentProfile.builder().id(99L).displayName("Other").grade(6).build();
        Subject sci = Subject.builder().id(4L).code(SubjectCode.SCI).name("KHTN").build();
        StudyPlan plan = StudyPlan.builder().id(30L).student(otherStudent).targetDate(LocalDate.of(2026, 11, 30)).build();
        StudyTask task = StudyTask.builder().id(105L).plan(plan).subject(sci).taskType(StudyTaskType.PREVIEW).status(StudyTaskStatus.EVALUATED).revisionCount(1).build();
        StudySubmission v2 = StudySubmission.builder().id(602L).task(task).version(2).isCurrent(true).lessonTitle("Áp suất").content("a".repeat(900)).charCount(900).submittedAt(Instant.now()).build();
        StudySubmission v1 = StudySubmission.builder().id(601L).task(task).version(1).isCurrent(false).lessonTitle("Áp suất").content("b".repeat(900)).charCount(900).submittedAt(Instant.now()).build();

        AIEvaluation evalV2 = AIEvaluation.builder()
                .id(802L)
                .submissionId(602L)
                .accuracy(AccuracyLevel.CORRECT)
                .completeness(CompletenessLevel.COMPLETE)
                .overallStatus(OverallEvaluationStatus.GOOD)
                .feedback("Đầy đủ ý")
                .confidence(ConfidenceLevel.HIGH)
                .sources(List.of(SourceReference.builder().id(1L).title("KNTT").url("https://loigiaihay.com").retrievedAt(Instant.now()).build()))
                .evaluatedAt(Instant.now())
                .build();

        when(studySubmissionRepository.findById(602L)).thenReturn(Optional.of(v2));
        when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(105L)).thenReturn(List.of(v2, v1));
        when(aiEvaluationRepository.findBySubmissionIdIn(List.of(602L, 601L))).thenReturn(List.of(evalV2));
        when(evaluationJobRepository.findBySubmissionIdIn(List.of(602L, 601L))).thenReturn(List.of());

        GuardianSubmissionDetailResponse detail = guardianService.getSubmissionDetail(2L, 602L);
        assertEquals(2, detail.versions().size());
        assertEquals(1, detail.versions().get(0).evaluation().sources().size());

        doThrow(new AppException(ErrorCode.GUARDIAN_NOT_LINKED))
                .when(studyAccessGuard).assertGuardianCanReadStudent(999L, 99L);

        AppException ex = assertThrows(
                AppException.class,
                () -> guardianService.getSubmissionDetail(999L, 602L)
        );
        assertEquals(ErrorCode.GUARDIAN_NOT_LINKED, ex.getErrorCode());
    }

    @Test
    void acknowledgeTaskWeeklySummaryAndExcelExportWorkWithoutAiUsageLeak() throws Exception {
        StudentProfile student = StudentProfile.builder()
                .id(10L)
                .displayName("Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        LocalDate weekStart = LocalDate.of(2026, 11, 23);

        StudyTask task = StudyTask.builder()
                .id(101L)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.EVALUATED)
                .revisionCount(1)
                .build();
        StudyPlan plan = StudyPlan.builder()
                .id(20L)
                .student(student)
                .targetDate(LocalDate.of(2026, 11, 24))
                .tasks(List.of(task))
                .build();
        task.setPlan(plan);

        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(task));
        when(studyTaskRepository.save(task)).thenReturn(task);

        var ack = guardianService.acknowledgeTask(
                2L,
                101L,
                new com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeRequest("  Con làm rất tốt!  ")
        );
        assertEquals(101L, ack.taskId());
        assertEquals("Con làm rất tốt!", ack.note());
        assertEquals("Con làm rất tốt!", task.getGuardianNote());

        when(studyAccessGuard.requireLinkedStudentForGuardian(2L)).thenReturn(student);
        when(studyPlanRepository.findAllByStudentIdAndTargetDateBetweenOrderByTargetDateDesc(
                10L, weekStart, weekStart.plusDays(6)
        )).thenReturn(List.of(plan));

        var weekly = guardianService.getWeeklySummary(2L, weekStart, null);
        assertEquals(1, weekly.totalTasks());
        assertEquals(1, weekly.completedTasks());
        assertEquals(1, weekly.acknowledgedTasks());
        assertEquals(7, weekly.days().size());

        when(studyPlanRepository.findAllByStudentIdAndTargetDateBetweenOrderByTargetDateDesc(
                10L, weekStart, LocalDate.of(2026, 11, 29)
        )).thenReturn(List.of(plan));
        byte[] xlsx = guardianService.exportExcelReport(2L, weekStart, LocalDate.of(2026, 11, 29), null);
        // ZIP / OOXML magic bytes 'PK'
        assertEquals((byte) 'P', xlsx[0]);
        assertEquals((byte) 'K', xlsx[1]);
    }

    @Test
    void skipAndRestoreReviewTaskUnlocksAndRelocksPreviewSafely() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(7).build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(LocalDate.of(2026, 11, 25)).build();
        StudyTask review = StudyTask.builder().id(101L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.NOT_STARTED).build();
        StudyTask preview = StudyTask.builder().id(102L).plan(plan).subject(math).taskType(StudyTaskType.PREVIEW).status(StudyTaskStatus.BLOCKED).build();

        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(review));
        when(studySubmissionRepository.existsByTaskId(101L)).thenReturn(false);
        when(studySubmissionRepository.existsByTaskId(102L)).thenReturn(false);
        when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(20L, 1L, StudyTaskType.PREVIEW))
                .thenReturn(Optional.of(preview));

        var skippedRes = guardianService.skipTask(
                2L, 101L, new com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskRequest(true, "  Đã thuộc trên lớp ")
        );
        assertEquals(StudyTaskStatus.SKIPPED, skippedRes.status());
        assertEquals("Đã thuộc trên lớp", skippedRes.guardianNote());
        assertEquals(StudyTaskStatus.NOT_STARTED, preview.getStatus());

        var restoredRes = guardianService.skipTask(
                2L, 101L, new com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskRequest(false, null)
        );
        assertEquals(StudyTaskStatus.NOT_STARTED, restoredRes.status());
        assertEquals(StudyTaskStatus.BLOCKED, preview.getStatus());

        // Cannot skip an already evaluated task
        StudyTask evaluated = StudyTask.builder().id(103L).plan(plan).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).build();
        when(studyTaskRepository.findById(103L)).thenReturn(Optional.of(evaluated));
        when(studySubmissionRepository.existsByTaskId(103L)).thenReturn(true);
        AppException rejectEx = assertThrows(AppException.class, () -> guardianService.skipTask(
                2L, 103L, new com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskRequest(true, null)
        ));
        assertEquals(ErrorCode.VALIDATION_FAILED, rejectEx.getErrorCode());
    }
}
