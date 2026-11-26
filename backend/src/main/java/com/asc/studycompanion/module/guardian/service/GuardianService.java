package com.asc.studycompanion.module.guardian.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianAcknowledgeResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianAssignCustomTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianEvaluationSummary;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipReviewRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianSkipTaskResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianTaskItem;
import com.asc.studycompanion.module.guardian.dto.GuardianVerifyCustomTaskRequest;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklyDaySummary;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklySubjectStat;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklySummaryResponse;
import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import com.asc.studycompanion.module.study.service.StudyPlanService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private static final List<String> WEEKDAY_LABELS = List.of(
            "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"
    );

    private final StudyAccessGuard studyAccessGuard;
    private final StudyPlanRepository studyPlanRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final AIEvaluationRepository aiEvaluationRepository;
    private final EvaluationJobRepository evaluationJobRepository;
    private final StudyTaskService studyTaskService;
    private final StudyTaskRepository studyTaskRepository;
    private final StudyPlanService studyPlanService;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone = "Asia/Ho_Chi_Minh";

    private Clock clock = Clock.systemUTC();

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public GuardianDashboardResponse getDashboard(
            Long guardianUserId,
            LocalDate requestedDate,
            String requestedSubject
    ) {
        return getDashboard(guardianUserId, requestedDate, requestedSubject, null);
    }

    @Transactional
    public GuardianDashboardResponse getDashboard(
            Long guardianUserId,
            LocalDate requestedDate,
            String requestedSubject,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = resolveZoneId(student);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate selectedDate = requestedDate != null
                ? requestedDate
                : today.plusDays(1);

        SubjectCode filterSubject = parseSubjectFilter(requestedSubject);
        String selectedSubjectLabel = filterSubject != null ? filterSubject.name() : "ALL";

        String defaultDriveFolderUrl = studyAccessGuard.findActiveGuardianLink(guardianUserId, student.getId())
                .map(GuardianLink::getDefaultDriveFolderUrl)
                .orElse(null);

        Optional<StudyPlan> planOpt = !selectedDate.isBefore(today)
                ? studyPlanService.ensurePlanForStudentAndDate(student, selectedDate)
                : studyPlanRepository.findByStudentIdAndTargetDate(student.getId(), selectedDate);
        if (planOpt.isEmpty()) {
            return new GuardianDashboardResponse(
                    student.getDisplayName(),
                    student.getGrade(),
                    selectedDate,
                    selectedSubjectLabel,
                    0,
                    0,
                    0,
                    List.of(),
                    defaultDriveFolderUrl
            );
        }

        StudyPlan plan = planOpt.get();
        List<StudyTask> filteredTasks = plan.getTasks().stream()
                .filter(task -> filterSubject == null || task.getSubject().getCode() == filterSubject)
                .toList();

        List<GuardianTaskItem> items = new ArrayList<>();
        int activeTotalCount = 0;
        int completedCount = 0;
        int evaluatingCount = 0;

        for (StudyTask task : filteredTasks) {
            if (task.getStatus() != StudyTaskStatus.SKIPPED) {
                activeTotalCount++;
            }
            if (task.getStatus() == StudyTaskStatus.EVALUATED) {
                completedCount++;
            } else if (task.getStatus() == StudyTaskStatus.EVALUATING || task.getStatus() == StudyTaskStatus.SUBMITTED) {
                evaluatingCount++;
            }

            Optional<StudySubmission> currentSubOpt = studySubmissionRepository
                    .findFirstByTaskIdAndIsCurrentTrue(task.getId());
            StudySubmission currentSub = currentSubOpt.orElse(null);
            GuardianEvaluationSummary evalSummary = currentSub != null
                    ? aiEvaluationRepository.findBySubmissionId(currentSub.getId())
                            .map(ev -> new GuardianEvaluationSummary(
                                    ev.getAccuracy(),
                                    ev.getCompleteness(),
                                    ev.getOverallStatus(),
                                    ev.getFeedback(),
                                    ev.getEvaluatedAt()
                            ))
                            .orElse(null)
                    : null;

            items.add(GuardianTaskItem.from(
                    task,
                    currentSub != null ? currentSub.getId() : null,
                    currentSub != null ? currentSub.getLessonTitle() : null,
                    evalSummary
            ));
        }

        return new GuardianDashboardResponse(
                student.getDisplayName(),
                student.getGrade(),
                selectedDate,
                selectedSubjectLabel,
                activeTotalCount,
                completedCount,
                evaluatingCount,
                items,
                defaultDriveFolderUrl
        );
    }

    @Transactional(readOnly = true)
    public GuardianSubmissionDetailResponse getSubmissionDetail(
            Long guardianUserId,
            Long submissionId
    ) {
        StudySubmission requested = studySubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        StudyTask task = requested.getTask();
        StudentProfile student = task.getPlan().getStudent();
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, student.getId());

        List<StudySubmission> allVersions = studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(task.getId());
        List<Long> versionIds = allVersions.stream().map(StudySubmission::getId).toList();

        Map<Long, AIEvaluation> evalsBySubId = versionIds.isEmpty()
                ? Map.of()
                : aiEvaluationRepository.findBySubmissionIdIn(versionIds)
                        .stream()
                        .collect(Collectors.toMap(AIEvaluation::getSubmissionId, Function.identity()));
        Map<Long, String> errorsBySubId = versionIds.isEmpty()
                ? Map.of()
                : evaluationJobRepository.findBySubmissionIdIn(versionIds)
                        .stream()
                        .filter(job -> job.getLastError() != null)
                        .collect(Collectors.toMap(EvaluationJob::getSubmissionId, EvaluationJob::getLastError));

        List<StudySubmissionResponse> versionDtos = allVersions.stream()
                .map(sub -> studyTaskService.toSubmissionResponse(
                        sub,
                        evalsBySubId.get(sub.getId()),
                        errorsBySubId.get(sub.getId())
                ))
                .toList();

        return new GuardianSubmissionDetailResponse(
                submissionId,
                task.getId(),
                student.getDisplayName(),
                task.getPlan().getTargetDate(),
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                task.getRevisionCount(),
                task.getGuardianAcknowledgedAt(),
                task.getGuardianNote(),
                versionDtos
        );
    }

    @Transactional
    public GuardianAcknowledgeResponse acknowledgeTask(
            Long guardianUserId,
            Long taskId,
            GuardianAcknowledgeRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, task.getPlan().getStudent().getId());

        String normalizedNote = (request != null && request.note() != null && !request.note().isBlank())
                ? request.note().trim()
                : null;

        Instant acknowledgedAt = Instant.now();
        task.setGuardianAcknowledgedAt(acknowledgedAt);
        task.setGuardianNote(normalizedNote);
        task.setGuardianAcknowledgedByUserId(guardianUserId);
        studyTaskRepository.save(task);

        return new GuardianAcknowledgeResponse(task.getId(), acknowledgedAt, normalizedNote);
    }

    @Transactional
    public GuardianSkipTaskResponse skipTask(
            Long guardianUserId,
            Long taskId,
            GuardianSkipTaskRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, task.getPlan().getStudent().getId());

        boolean shouldSkip = request == null || request.skipped();
        assertTaskHasNoSubmission(task, shouldSkip
                ? "Không thể bỏ qua bài học đã được học sinh nộp"
                : "Không thể khôi phục bài học đã có bài nộp");

        String normalizedNote = (request != null && request.note() != null)
                ? (request.note().isBlank() ? null : request.note().trim())
                : null;

        if (shouldSkip) {
            if (task.getSkipRequestStatus() == SkipRequestStatus.PENDING) {
                task.setSkipRequestStatus(SkipRequestStatus.APPROVED);
                task.setSkipReviewedAt(Instant.now());
                task.setSkipReviewedByUserId(guardianUserId);
            }
            applySkipDecision(task, true, normalizedNote, false);
        } else {
            task.setSkipRequestStatus(null);
            task.setSkipRequestReason(null);
            task.setSkipRequestedAt(null);
            task.setSkipReviewedAt(null);
            task.setSkipReviewedByUserId(null);
            applySkipDecision(task, false, null, true);
        }

        return toSkipResponse(task);
    }

    @Transactional
    public GuardianSkipTaskResponse reviewSkipRequest(
            Long guardianUserId,
            Long taskId,
            GuardianSkipReviewRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, task.getPlan().getStudent().getId());

        if (task.getSkipRequestStatus() != SkipRequestStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu xin bỏ qua này không còn ở trạng thái chờ duyệt");
        }
        assertTaskHasNoSubmission(task, "Học sinh đã nộp bài cho nhiệm vụ này");

        boolean approved = request != null && Boolean.TRUE.equals(request.approved());
        String normalizedNote = (request != null && request.note() != null && !request.note().isBlank())
                ? request.note().trim()
                : null;

        task.setSkipReviewedAt(Instant.now());
        task.setSkipReviewedByUserId(guardianUserId);

        if (approved) {
            task.setSkipRequestStatus(SkipRequestStatus.APPROVED);
            applySkipDecision(task, true, normalizedNote, false);
        } else {
            task.setSkipRequestStatus(SkipRequestStatus.REJECTED);
            task.setGuardianNote(normalizedNote);
            studyTaskRepository.save(task);
        }

        return toSkipResponse(task);
    }

    private void assertTaskHasNoSubmission(StudyTask task, String message) {
        boolean hasSubmission = studySubmissionRepository.existsByTaskId(task.getId())
                || task.getCustomSubmittedAt() != null
                || task.getStatus() == StudyTaskStatus.SUBMITTED
                || task.getStatus() == StudyTaskStatus.EVALUATING
                || task.getStatus() == StudyTaskStatus.EVALUATED
                || task.getStatus() == StudyTaskStatus.EVALUATION_FAILED;
        if (hasSubmission) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, message);
        }
    }

    private void applySkipDecision(
            StudyTask task,
            boolean shouldSkip,
            String note,
            boolean clearNoteWhenRestoring
    ) {
        if (shouldSkip) {
            task.setStatus(StudyTaskStatus.SKIPPED);
            if (note != null) {
                task.setGuardianNote(note);
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
            return;
        }

        if (clearNoteWhenRestoring) {
            task.setGuardianNote(null);
        }
        if (task.getTaskType() == StudyTaskType.REVIEW) {
            task.setStatus(StudyTaskStatus.NOT_STARTED);
            studyTaskRepository.save(task);

            studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                            task.getPlan().getId(),
                            task.getSubject().getId(),
                            StudyTaskType.PREVIEW
                    )
                    .ifPresent(preview -> {
                        if (preview.getStatus() == StudyTaskStatus.NOT_STARTED
                                && !studySubmissionRepository.existsByTaskId(preview.getId())) {
                            preview.setStatus(StudyTaskStatus.BLOCKED);
                            studyTaskRepository.save(preview);
                        }
                    });
        } else {
            boolean reviewCleared = studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(
                            task.getPlan().getId(),
                            task.getSubject().getId(),
                            StudyTaskType.REVIEW
                    )
                    .map(review -> review.getStatus() == StudyTaskStatus.SKIPPED
                            || studySubmissionRepository.existsByTaskId(review.getId()))
                    .orElse(true);

            task.setStatus(reviewCleared ? StudyTaskStatus.NOT_STARTED : StudyTaskStatus.BLOCKED);
            studyTaskRepository.save(task);
        }
    }

    private static GuardianSkipTaskResponse toSkipResponse(StudyTask task) {
        return new GuardianSkipTaskResponse(
                task.getId(),
                task.getStatus(),
                task.getGuardianNote(),
                task.getSkipRequestStatus(),
                task.getSkipRequestReason(),
                task.getSkipRequestedAt(),
                task.getSkipReviewedAt()
        );
    }

    @Transactional
    public GuardianTaskItem assignCustomTask(
            Long guardianUserId,
            Long taskId,
            GuardianAssignCustomTaskRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        Long studentId = task.getPlan().getStudent().getId();
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, studentId);

        assertTaskHasNoSubmission(task, "Không thể giao bài thay thế cho nhiệm vụ đã có bài nộp");

        String normalizedTitle = request != null && request.title() != null ? request.title().trim() : "";
        if (normalizedTitle.length() < 5 || normalizedTitle.length() > 200) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Tên nhiệm vụ thay thế phải từ 5 đến 200 ký tự");
        }
        String normalizedDesc = (request != null && request.description() != null && !request.description().isBlank())
                ? request.description().trim()
                : null;
        if (normalizedDesc != null && normalizedDesc.length() > 500) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Hướng dẫn chi tiết tối đa 500 ký tự");
        }
        String normalizedDriveUrl = normalizeOptionalDriveFolderUrl(request != null ? request.driveFolderUrl() : null);

        if (task.getStatus() == StudyTaskStatus.SKIPPED) {
            applySkipDecision(task, false, null, false);
        }
        if (task.getSkipRequestStatus() == SkipRequestStatus.PENDING) {
            task.setSkipRequestStatus(null);
            task.setSkipRequestReason(null);
            task.setSkipRequestedAt(null);
            task.setSkipReviewedAt(null);
            task.setSkipReviewedByUserId(null);
        }

        task.setAssignmentMode(TaskAssignmentMode.CUSTOM_DRIVE);
        task.setCustomTaskTitle(normalizedTitle);
        task.setCustomTaskDescription(normalizedDesc);
        task.setCustomDriveFolderUrl(normalizedDriveUrl);
        task.setCustomAssignedByUserId(guardianUserId);
        task.setCustomAssignedAt(Instant.now());
        studyTaskRepository.save(task);

        if (normalizedDriveUrl != null && Boolean.TRUE.equals(request.saveAsDefaultDriveFolder())) {
            studyAccessGuard.updateDefaultDriveFolderUrl(guardianUserId, studentId, normalizedDriveUrl);
        }

        return GuardianTaskItem.from(task, null, null, null);
    }

    @Transactional
    public GuardianTaskItem clearCustomTask(Long guardianUserId, Long taskId) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, task.getPlan().getStudent().getId());

        if (task.getAssignmentMode() != TaskAssignmentMode.CUSTOM_DRIVE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Nhiệm vụ này đang ở chế độ AI mặc định");
        }
        if (task.getCustomSubmittedAt() != null
                || task.getStatus() == StudyTaskStatus.SUBMITTED
                || task.getStatus() == StudyTaskStatus.EVALUATED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Không thể gỡ bài giao thay thế sau khi con đã nộp bài");
        }

        task.setAssignmentMode(TaskAssignmentMode.STANDARD_AI);
        task.setCustomTaskTitle(null);
        task.setCustomTaskDescription(null);
        task.setCustomDriveFolderUrl(null);
        task.setCustomAssignedByUserId(null);
        task.setCustomAssignedAt(null);
        task.setCustomSubmissionNote(null);
        task.setCustomSubmissionUrl(null);
        task.setCustomSubmittedAt(null);
        studyTaskRepository.save(task);

        return GuardianTaskItem.from(task, null, null, null);
    }

    @Transactional
    public GuardianTaskItem verifyCustomTask(
            Long guardianUserId,
            Long taskId,
            GuardianVerifyCustomTaskRequest request
    ) {
        StudyTask task = studyTaskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDY_TASK_NOT_FOUND));
        studyAccessGuard.assertGuardianCanReadStudent(guardianUserId, task.getPlan().getStudent().getId());

        if (task.getAssignmentMode() != TaskAssignmentMode.CUSTOM_DRIVE
                || task.getStatus() != StudyTaskStatus.SUBMITTED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ có thể xác nhận bài giao thay thế đang chờ kiểm tra trên Drive");
        }

        boolean approved = request != null && Boolean.TRUE.equals(request.approved());
        String normalizedNote = (request != null && request.note() != null && !request.note().isBlank())
                ? request.note().trim()
                : null;

        if (approved) {
            task.setStatus(StudyTaskStatus.EVALUATED);
            task.setGuardianAcknowledgedAt(Instant.now());
            task.setGuardianAcknowledgedByUserId(guardianUserId);
            if (normalizedNote != null) {
                task.setGuardianNote(normalizedNote);
            }
        } else {
            task.setStatus(StudyTaskStatus.IN_PROGRESS);
            task.setGuardianNote(normalizedNote);
        }
        studyTaskRepository.save(task);

        return GuardianTaskItem.from(task, null, null, null);
    }

    private static String normalizeOptionalDriveFolderUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > 500) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Link thư mục Google Drive không hợp lệ (tối đa 500 ký tự)");
        }
        try {
            URI uri = URI.create(trimmed);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Link thư mục Google Drive phải bắt đầu bằng https://");
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Link thư mục Google Drive không đúng định dạng URL");
        }
    }

    @Transactional
    public GuardianWeeklySummaryResponse getWeeklySummary(
            Long guardianUserId,
            LocalDate requestedWeekStart,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = resolveZoneId(student);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekStart = requestedWeekStart != null
                ? requestedWeekStart
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        ensureUpcomingPlansInRange(student, weekStart, weekEnd, today, tomorrow);

        List<StudyPlan> plans = studyPlanRepository
                .findAllByStudentIdAndTargetDateBetweenOrderByTargetDateDesc(student.getId(), weekStart, weekEnd);
        Map<LocalDate, StudyPlan> planByDate = plans.stream()
                .collect(Collectors.toMap(StudyPlan::getTargetDate, Function.identity(), (a, b) -> a));

        List<GuardianWeeklyDaySummary> days = new ArrayList<>();
        Map<SubjectCode, int[]> subjectCounts = new LinkedHashMap<>();
        Map<SubjectCode, String> subjectNames = new LinkedHashMap<>();
        int totalTasks = 0;
        int completedTasks = 0;
        int acknowledgedTasks = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            String weekdayLabel = WEEKDAY_LABELS.get(day.getDayOfWeek().getValue() - 1);
            StudyPlan plan = planByDate.get(day);
            int dayTotal = 0;
            int dayCompleted = 0;
            int dayAcknowledged = 0;
            if (plan != null) {
                for (StudyTask task : plan.getTasks()) {
                    boolean isSkipped = task.getStatus() == StudyTaskStatus.SKIPPED;
                    boolean isDone = task.getStatus() == StudyTaskStatus.EVALUATED;
                    if (!isSkipped) {
                        dayTotal++;
                    }
                    if (isDone) {
                        dayCompleted++;
                    }
                    if (task.getGuardianAcknowledgedAt() != null) {
                        dayAcknowledged++;
                    }
                    SubjectCode code = task.getSubject().getCode();
                    subjectNames.putIfAbsent(code, task.getSubject().getName());
                    int[] counts = subjectCounts.computeIfAbsent(code, k -> new int[2]);
                    if (!isSkipped) {
                        counts[0]++;
                    }
                    if (isDone) {
                        counts[1]++;
                    }
                }
            }
            totalTasks += dayTotal;
            completedTasks += dayCompleted;
            acknowledgedTasks += dayAcknowledged;
            days.add(new GuardianWeeklyDaySummary(day, weekdayLabel, dayTotal, dayCompleted, dayAcknowledged));
        }

        List<GuardianWeeklySubjectStat> subjects = subjectCounts.entrySet().stream()
                .map(entry -> new GuardianWeeklySubjectStat(
                        entry.getKey(),
                        subjectNames.get(entry.getKey()),
                        entry.getValue()[0],
                        entry.getValue()[1]
                ))
                .toList();

        return new GuardianWeeklySummaryResponse(
                student.getId(),
                student.getDisplayName(),
                student.getGrade(),
                weekStart,
                weekEnd,
                totalTasks,
                completedTasks,
                acknowledgedTasks,
                days,
                subjects
        );
    }

    @Transactional
    public byte[] exportExcelReport(
            Long guardianUserId,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = resolveZoneId(student);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate tomorrow = today.plusDays(1);
        LocalDate toDate = requestedTo != null ? requestedTo : tomorrow;
        LocalDate fromDate = requestedFrom != null ? requestedFrom : toDate.minusDays(13);
        if (fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        ensureUpcomingPlansInRange(student, fromDate, toDate, today, tomorrow);

        List<StudyPlan> plans = studyPlanRepository
                .findAllByStudentIdAndTargetDateBetweenOrderByTargetDateDesc(student.getId(), fromDate, toDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BaoCaoHocTap");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Ngày học", "Môn học", "Loại nhiệm vụ", "Trạng thái",
                    "Tên bài học", "Đánh giá AI", "Số lần sửa",
                    "Phụ huynh xác nhận", "Lời nhắn động viên"
            };
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(col, 22 * 256);
            }

            int rowIdx = 1;
            for (StudyPlan plan : plans) {
                List<StudyTask> sortedTasks = plan.getTasks().stream()
                        .sorted(Comparator.comparing((StudyTask t) -> t.getSubject().getName())
                                .thenComparing(t -> t.getTaskType().name()))
                        .toList();
                for (StudyTask task : sortedTasks) {
                    StudySubmission currentSub = studySubmissionRepository
                            .findFirstByTaskIdAndIsCurrentTrue(task.getId())
                            .orElse(null);
                    AIEvaluation eval = currentSub != null
                            ? aiEvaluationRepository.findBySubmissionId(currentSub.getId()).orElse(null)
                            : null;

                    boolean isCustomDrive = task.getAssignmentMode() == TaskAssignmentMode.CUSTOM_DRIVE;
                    boolean hasCustomDriveUrl = task.getCustomDriveFolderUrl() != null
                            && !task.getCustomDriveFolderUrl().isBlank();
                    String lessonCellValue = currentSub != null
                            ? currentSub.getLessonTitle()
                            : (isCustomDrive && task.getCustomTaskTitle() != null
                                    ? "[PH giao] " + task.getCustomTaskTitle()
                                    : "Chưa nộp");
                    String evalCellValue = eval != null
                            ? eval.getOverallStatus().name()
                            : (isCustomDrive
                                    ? (task.getStatus() == StudyTaskStatus.EVALUATED
                                            ? (hasCustomDriveUrl ? "PH đã xác nhận (Drive)" : "PH đã xác nhận (Thủ công)")
                                            : (hasCustomDriveUrl ? "Nộp qua Google Drive" : "Nhiệm vụ PH giao (Thủ công)"))
                                    : "-");

                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(plan.getTargetDate().toString());
                    row.createCell(1).setCellValue(task.getSubject().getName());
                    row.createCell(2).setCellValue(task.getTaskType() == StudyTaskType.REVIEW ? "Ôn bài cũ" : "Soạn bài mới");
                    row.createCell(3).setCellValue(
                            task.getStatus() == StudyTaskStatus.SKIPPED
                                    ? "Đã bỏ qua (PH)"
                                    : task.getStatus().name()
                    );
                    row.createCell(4).setCellValue(lessonCellValue);
                    row.createCell(5).setCellValue(evalCellValue);
                    row.createCell(6).setCellValue(task.getRevisionCount() != null ? task.getRevisionCount() : 0);
                    row.createCell(7).setCellValue(task.getGuardianAcknowledgedAt() != null ? "Đã xem" : "Chưa xem");
                    row.createCell(8).setCellValue(task.getGuardianNote() != null ? task.getGuardianNote() : "");
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate Excel study report", ex);
        }
    }

    private SubjectCode parseSubjectFilter(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return SubjectCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_SUBJECT_CODE);
        }
    }

    private void ensureUpcomingPlansInRange(
            StudentProfile student,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate today,
            LocalDate tomorrow
    ) {
        if (!today.isBefore(fromDate) && !today.isAfter(toDate)) {
            studyPlanService.ensurePlanForStudentAndDate(student, today);
        }
        if (!tomorrow.isBefore(fromDate) && !tomorrow.isAfter(toDate)) {
            studyPlanService.ensurePlanForStudentAndDate(student, tomorrow);
        }
    }

    private ZoneId resolveZoneId(StudentProfile student) {
        try {
            return ZoneId.of(resolveTimezone(student));
        } catch (DateTimeException ex) {
            return ZoneId.of(appTimezone);
        }
    }

    private String resolveTimezone(StudentProfile student) {
        return (student.getTimezone() != null && !student.getTimezone().isBlank())
                ? student.getTimezone().trim()
                : appTimezone;
    }
}
