package com.asc.studycompanion.module.study;

import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.NextDayStudyPlanResponse;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.service.StudyPlanService;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableEntry;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.asc.studycompanion.module.timetable.repository.TimetableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private TimetableRepository timetableRepository;
    @Mock
    private StudySubmissionRepository studySubmissionRepository;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;

    private StudyPlanService studyPlanService;

    @BeforeEach
    void setUp() {
        studyPlanService = new StudyPlanService(
                studyAccessGuard,
                studyPlanRepository,
                timetableRepository,
                studySubmissionRepository,
                aiEvaluationRepository
        );
        // 2026-11-29 22:00 Asia/Ho_Chi_Minh (Sunday) -> Tomorrow is Monday 2026-11-30
        studyPlanService.setClock(Clock.fixed(Instant.parse("2026-11-29T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void sundayPreparesMondaySubjectsWithSingleReviewPreviewPairPerSubject() {
        StudentProfile student = StudentProfile.builder()
                .id(10L)
                .displayName("Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        Timetable activeTimetable = Timetable.builder()
                .id(5L)
                .student(student)
                .version(1)
                .status(TimetableStatus.ACTIVE)
                .entries(List.of(
                        TimetableEntry.builder().id(1L).weekday(1).subject(math).periodNote("Tiết 1").build(),
                        TimetableEntry.builder().id(2L).weekday(1).subject(math).periodNote("Tiết 4").build(),
                        TimetableEntry.builder().id(3L).weekday(2).subject(math).periodNote("Thứ 3").build()
                ))
                .build();

        when(studyAccessGuard.requireOwnedStudentProfile(1L)).thenReturn(student);
        when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(10L, TimetableStatus.ACTIVE))
                .thenReturn(Optional.of(activeTimetable));
        when(studyPlanRepository.findByStudentIdAndTargetDate(10L, LocalDate.of(2026, 11, 30)))
                .thenReturn(Optional.empty());
        when(studyPlanRepository.saveAndFlush(any(StudyPlan.class))).thenAnswer(inv -> {
            StudyPlan plan = inv.getArgument(0);
            plan.setId(77L);
            long taskId = 100L;
            for (StudyTask t : plan.getTasks()) {
                t.setId(taskId++);
            }
            return plan;
        });

        NextDayStudyPlanResponse response = studyPlanService.getNextDayPlan(1L);

        assertEquals(77L, response.planId());
        assertEquals(LocalDate.of(2026, 11, 30), response.targetDate());
        assertEquals("Thứ 2", response.weekdayLabel());
        assertTrue(response.hasSchedule());
        assertEquals(1, response.subjects().size());
        assertEquals("Tiết 1, Tiết 4", response.subjects().get(0).periodNote());
        assertEquals(StudyTaskStatus.NOT_STARTED, response.subjects().get(0).reviewTask().status());
        assertEquals(StudyTaskStatus.BLOCKED, response.subjects().get(0).previewTask().status());
        assertEquals(2, response.subjects().get(0).reviewTask().maxRevisions());
    }

    @Test
    void returnsEmptyScheduleWhenTomorrowHasNoEntries() {
        StudentProfile student = StudentProfile.builder()
                .id(10L)
                .displayName("Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        when(studyAccessGuard.requireOwnedStudentProfile(1L)).thenReturn(student);
        when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(10L, TimetableStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(studyPlanRepository.findByStudentIdAndTargetDate(10L, LocalDate.of(2026, 11, 30)))
                .thenReturn(Optional.empty());

        NextDayStudyPlanResponse response = studyPlanService.getNextDayPlan(1L);

        assertNull(response.planId());
        assertFalse(response.hasSchedule());
        assertTrue(response.subjects().isEmpty());
        verify(studyPlanRepository, never()).saveAndFlush(any());
    }

    @Test
    void preservesExistingSubmittedPlanWhenTimetableChanges() {
        StudentProfile student = StudentProfile.builder().id(10L).timezone("Asia/Ho_Chi_Minh").build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        Timetable oldTimetable = Timetable.builder().id(5L).version(1).entries(List.of()).build();
        Timetable newTimetable = Timetable.builder().id(6L).version(2).entries(List.of()).build();

        StudyPlan existingPlan = StudyPlan.builder()
                .id(80L)
                .student(student)
                .targetDate(LocalDate.of(2026, 11, 30))
                .timetable(oldTimetable)
                .tasks(List.of(
                        StudyTask.builder().id(1L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).revisionCount(0).build(),
                        StudyTask.builder().id(2L).subject(math).taskType(StudyTaskType.PREVIEW).status(StudyTaskStatus.NOT_STARTED).revisionCount(0).build()
                ))
                .build();

        when(studyAccessGuard.requireOwnedStudentProfile(1L)).thenReturn(student);
        when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(10L, TimetableStatus.ACTIVE))
                .thenReturn(Optional.of(newTimetable));
        when(studyPlanRepository.findByStudentIdAndTargetDate(10L, LocalDate.of(2026, 11, 30)))
                .thenReturn(Optional.of(existingPlan));

        NextDayStudyPlanResponse response = studyPlanService.getNextDayPlan(1L);

        assertEquals(80L, response.planId());
        assertEquals(1, response.subjects().size());
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void populatesCurrentLessonTitleAndLatestEvaluationStatusOnTaskSummary() {
        StudentProfile student = StudentProfile.builder().id(10L).timezone("Asia/Ho_Chi_Minh").build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        Timetable activeTimetable = Timetable.builder().id(6L).version(1).entries(List.of()).build();

        StudyTask reviewTask = StudyTask.builder()
                .id(11L)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.EVALUATED)
                .revisionCount(0)
                .build();
        StudyTask previewTask = StudyTask.builder()
                .id(12L)
                .subject(math)
                .taskType(StudyTaskType.PREVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .revisionCount(0)
                .build();
        StudyPlan existingPlan = StudyPlan.builder()
                .id(81L)
                .student(student)
                .targetDate(LocalDate.of(2026, 11, 30))
                .timetable(activeTimetable)
                .tasks(List.of(reviewTask, previewTask))
                .build();

        StudySubmission submission = StudySubmission.builder()
                .id(101L)
                .task(reviewTask)
                .version(1)
                .lessonTitle("Bài 2: Tập hợp các số tự nhiên")
                .isCurrent(true)
                .build();
        AIEvaluation evaluation = AIEvaluation.builder()
                .id(201L)
                .submissionId(101L)
                .overallStatus(OverallEvaluationStatus.GOOD)
                .build();

        when(studyAccessGuard.requireOwnedStudentProfile(1L)).thenReturn(student);
        when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(10L, TimetableStatus.ACTIVE))
                .thenReturn(Optional.of(activeTimetable));
        when(studyPlanRepository.findByStudentIdAndTargetDate(10L, LocalDate.of(2026, 11, 30)))
                .thenReturn(Optional.of(existingPlan));
        when(studySubmissionRepository.findFirstByTaskIdAndIsCurrentTrue(11L))
                .thenReturn(Optional.of(submission));
        when(studySubmissionRepository.findFirstByTaskIdAndIsCurrentTrue(12L))
                .thenReturn(Optional.empty());
        when(aiEvaluationRepository.findBySubmissionId(101L))
                .thenReturn(Optional.of(evaluation));

        NextDayStudyPlanResponse response = studyPlanService.getNextDayPlan(1L);

        assertEquals("Bài 2: Tập hợp các số tự nhiên", response.subjects().get(0).reviewTask().currentLessonTitle());
        assertEquals(OverallEvaluationStatus.GOOD, response.subjects().get(0).reviewTask().latestEvaluationStatus());
        assertNull(response.subjects().get(0).previewTask().currentLessonTitle());
        assertNull(response.subjects().get(0).previewTask().latestEvaluationStatus());
    }

    @Test
    void calculatesHistoryAndConsecutiveStudyStreakAcrossDays() {
        StudentProfile student = StudentProfile.builder()
                .id(10L)
                .displayName("Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();

        StudyTask done1 = StudyTask.builder().id(1L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).build();
        StudyTask done2 = StudyTask.builder().id(2L).subject(math).taskType(StudyTaskType.PREVIEW).status(StudyTaskStatus.NOT_STARTED).build();
        StudyTask done3 = StudyTask.builder().id(3L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).build();
        StudyTask done4 = StudyTask.builder().id(4L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).build();
        StudyTask oldDone = StudyTask.builder().id(5L).subject(math).taskType(StudyTaskType.REVIEW).status(StudyTaskStatus.EVALUATED).build();

        // Clock is 2026-11-29 (Sunday). Tomorrow = 2026-11-30.
        // Consecutive completed days: 11-28, 11-29, 11-30 (streak = 3) + gap + older 4-day streak (11-20..11-23)
        List<StudyPlan> plans = List.of(
                StudyPlan.builder().id(1L).student(student).targetDate(LocalDate.of(2026, 11, 30)).tasks(List.of(done1, done2)).build(),
                StudyPlan.builder().id(2L).student(student).targetDate(LocalDate.of(2026, 11, 29)).tasks(List.of(done3)).build(),
                StudyPlan.builder().id(3L).student(student).targetDate(LocalDate.of(2026, 11, 28)).tasks(List.of(done4)).build(),
                StudyPlan.builder().id(4L).student(student).targetDate(LocalDate.of(2026, 11, 23)).tasks(List.of(oldDone)).build(),
                StudyPlan.builder().id(5L).student(student).targetDate(LocalDate.of(2026, 11, 22)).tasks(List.of(oldDone)).build(),
                StudyPlan.builder().id(6L).student(student).targetDate(LocalDate.of(2026, 11, 21)).tasks(List.of(oldDone)).build(),
                StudyPlan.builder().id(7L).student(student).targetDate(LocalDate.of(2026, 11, 20)).tasks(List.of(oldDone)).build()
        );

        when(studyAccessGuard.requireOwnedStudentProfile(1L)).thenReturn(student);
        when(studyPlanRepository.findAllByStudentIdOrderByTargetDateDesc(10L)).thenReturn(plans);

        var streak = studyPlanService.getStreak(1L);
        assertEquals(3, streak.currentStreak());
        assertEquals(4, streak.longestStreak());
        assertEquals(7, streak.totalCompletedDays());
        assertEquals(LocalDate.of(2026, 11, 30), streak.lastActiveDate());

        var history = studyPlanService.getPlanHistory(1L, null, null);
        assertEquals(7, history.size());
        assertEquals(2, history.get(0).totalTasks());
        assertEquals(1, history.get(0).completedTasks());
    }
}
