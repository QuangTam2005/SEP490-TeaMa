package com.asc.studycompanion.module.study;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.StudentCustomSubmitRequest;
import com.asc.studycompanion.module.study.dto.StudentSkipRequestDto;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyTaskServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private StudyTaskRepository studyTaskRepository;
    @Mock
    private StudySubmissionRepository studySubmissionRepository;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;
    @Mock
    private EvaluationJobRepository evaluationJobRepository;

    private StudyTaskService studyTaskService;

    @BeforeEach
    void setUp() {
        studyTaskService = new StudyTaskService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository
        );
    }

    @Test
    void studentCanRequestAndCancelSkipOnBlockedOrNotStartedTasks() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(6).build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(LocalDate.of(2026, 11, 27)).build();
        StudyTask previewBlocked = StudyTask.builder()
                .id(102L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.PREVIEW)
                .status(StudyTaskStatus.BLOCKED)
                .revisionCount(0)
                .build();

        when(studyTaskRepository.findById(102L)).thenReturn(Optional.of(previewBlocked));
        when(studySubmissionRepository.existsByTaskId(102L)).thenReturn(false);

        StudyTaskSummaryResponse requested = studyTaskService.requestSkip(
                1L,
                102L,
                new StudentSkipRequestDto("  Làm bài tập thầy cô giao trên lớp  ")
        );

        assertEquals(StudyTaskStatus.BLOCKED, requested.status());
        assertEquals(SkipRequestStatus.PENDING, requested.skipRequestStatus());
        assertEquals("Làm bài tập thầy cô giao trên lớp", requested.skipRequestReason());
        assertNotNull(requested.skipRequestedAt());

        StudyTaskSummaryResponse cancelled = studyTaskService.cancelSkipRequest(1L, 102L);
        assertNull(cancelled.skipRequestStatus());
        assertNull(cancelled.skipRequestReason());
        assertNull(cancelled.skipRequestedAt());
    }

    @Test
    void rejectsSkipRequestWithoutLinkedGuardianOrAfterSubmission() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(6).build();
        Subject lit = Subject.builder().id(2L).code(SubjectCode.LIT).name("Ngữ văn").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(LocalDate.of(2026, 11, 27)).build();
        StudyTask review = StudyTask.builder()
                .id(103L)
                .plan(plan)
                .subject(lit)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .revisionCount(0)
                .build();

        when(studyTaskRepository.findById(103L)).thenReturn(Optional.of(review));
        doThrow(new AppException(ErrorCode.VALIDATION_FAILED, "Chưa liên kết"))
                .when(studyAccessGuard).assertStudentHasActiveGuardian(10L);

        AppException noGuardian = assertThrows(
                AppException.class,
                () -> studyTaskService.requestSkip(1L, 103L, new StudentSkipRequestDto("Làm đề cương"))
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, noGuardian.getErrorCode());

        StudyTask evaluated = StudyTask.builder()
                .id(104L)
                .plan(plan)
                .subject(lit)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.EVALUATED)
                .revisionCount(0)
                .build();
        StudentProfile linkedStudent = StudentProfile.builder().id(11L).displayName("An").grade(6).build();
        plan.setStudent(linkedStudent);
        when(studyTaskRepository.findById(104L)).thenReturn(Optional.of(evaluated));
        when(studySubmissionRepository.existsByTaskId(104L)).thenReturn(true);

        AppException alreadyDone = assertThrows(
                AppException.class,
                () -> studyTaskService.requestSkip(1L, 104L, new StudentSkipRequestDto("Làm đề cương"))
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, alreadyDone.getErrorCode());
    }

    @Test
    void studentCustomDriveSubmissionMarksSubmittedAndUnlocksPreviewSibling() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(6).build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(LocalDate.of(2026, 11, 27)).build();
        StudyTask reviewCustom = StudyTask.builder()
                .id(101L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .assignmentMode(TaskAssignmentMode.CUSTOM_DRIVE)
                .customTaskTitle("Làm 5 bài SBT")
                .customDriveFolderUrl("https://drive.google.com/drive/folders/abc")
                .build();
        StudyTask preview = StudyTask.builder()
                .id(102L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.PREVIEW)
                .status(StudyTaskStatus.BLOCKED)
                .build();

        when(studyTaskRepository.findById(101L)).thenReturn(Optional.of(reviewCustom));
        when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(20L, 1L, StudyTaskType.PREVIEW))
                .thenReturn(Optional.of(preview));

        StudyTaskSummaryResponse res = studyTaskService.submitCustomTask(
                1L,
                101L,
                new StudentCustomSubmitRequest("Con đã up 2 ảnh bài làm lên Drive", "https://drive.google.com/file/d/xyz/view")
        );

        assertEquals(StudyTaskStatus.SUBMITTED, res.status());
        assertEquals(TaskAssignmentMode.CUSTOM_DRIVE, res.assignmentMode());
        assertEquals("Con đã up 2 ảnh bài làm lên Drive", res.customSubmissionNote());
        assertNotNull(res.customSubmittedAt());
        assertEquals(StudyTaskStatus.NOT_STARTED, preview.getStatus());
    }

    @Test
    void enforcesAntiIdorReviewGateAndHttpsDriveUrlValidationOnSkipAndCustomTasks() {
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Khoi").grade(6).build();
        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").build();
        StudyPlan plan = StudyPlan.builder().id(20L).student(student).targetDate(LocalDate.of(2026, 11, 27)).build();

        StudyTask reviewUnfinished = StudyTask.builder()
                .id(201L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.REVIEW)
                .status(StudyTaskStatus.NOT_STARTED)
                .assignmentMode(TaskAssignmentMode.CUSTOM_DRIVE)
                .build();
        StudyTask previewCustom = StudyTask.builder()
                .id(202L)
                .plan(plan)
                .subject(math)
                .taskType(StudyTaskType.PREVIEW)
                .status(StudyTaskStatus.BLOCKED)
                .assignmentMode(TaskAssignmentMode.CUSTOM_DRIVE)
                .build();

        // Anti-IDOR: another student cannot requestSkip, cancelSkipRequest, or submitCustomTask
        when(studyTaskRepository.findById(201L)).thenReturn(Optional.of(reviewUnfinished));
        doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(studyAccessGuard).assertStudentOwner(999L, 10L);

        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> studyTaskService.requestSkip(999L, 201L, new StudentSkipRequestDto("Xin nghỉ hôm nay"))).getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> studyTaskService.cancelSkipRequest(999L, 201L)).getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> studyTaskService.submitCustomTask(999L, 201L, new StudentCustomSubmitRequest("Con đã nộp", null))).getErrorCode());

        // PREVIEW custom-drive task blocked until REVIEW is completed
        when(studyTaskRepository.findById(202L)).thenReturn(Optional.of(previewCustom));
        when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(20L, 1L, StudyTaskType.REVIEW))
                .thenReturn(Optional.of(reviewUnfinished));
        when(studySubmissionRepository.existsByTaskId(201L)).thenReturn(false);

        AppException previewGateEx = assertThrows(AppException.class,
                () -> studyTaskService.submitCustomTask(1L, 202L, new StudentCustomSubmitRequest("Con đã soạn bài mới", null)));
        assertEquals(ErrorCode.REVIEW_REQUIRED_BEFORE_PREVIEW, previewGateEx.getErrorCode());

        // HTTPS-only and <= 500 chars validation on optional driveFileUrl
        AppException httpEx = assertThrows(AppException.class,
                () -> studyTaskService.submitCustomTask(1L, 201L,
                        new StudentCustomSubmitRequest("Con đã nộp bài", "http://drive.google.com/file/d/abc")));
        assertEquals(ErrorCode.VALIDATION_FAILED, httpEx.getErrorCode());

        AppException tooLongUrlEx = assertThrows(AppException.class,
                () -> studyTaskService.submitCustomTask(1L, 201L,
                        new StudentCustomSubmitRequest("Con đã nộp bài", "https://drive.google.com/" + "a".repeat(480))));
        assertEquals(ErrorCode.VALIDATION_FAILED, tooLongUrlEx.getErrorCode());

        AppException badSchemeEx = assertThrows(AppException.class,
                () -> studyTaskService.submitCustomTask(1L, 201L,
                        new StudentCustomSubmitRequest("Con đã nộp bài", "javascript:alert(1)")));
        assertEquals(ErrorCode.VALIDATION_FAILED, badSchemeEx.getErrorCode());
    }
}
