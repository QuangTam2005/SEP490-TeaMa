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
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianEvaluationSummary;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianTaskItem;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklyDaySummary;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklySubjectStat;
import com.asc.studycompanion.module.guardian.dto.GuardianWeeklySummaryResponse;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
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

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone = "Asia/Ho_Chi_Minh";

    @Transactional(readOnly = true)
    public GuardianDashboardResponse getDashboard(
            Long guardianUserId,
            LocalDate requestedDate,
            String requestedSubject
    ) {
        return getDashboard(guardianUserId, requestedDate, requestedSubject, null);
    }

    @Transactional(readOnly = true)
    public GuardianDashboardResponse getDashboard(
            Long guardianUserId,
            LocalDate requestedDate,
            String requestedSubject,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = ZoneId.of(resolveTimezone(student));
        LocalDate selectedDate = requestedDate != null
                ? requestedDate
                : LocalDate.now(zone).plusDays(1);

        SubjectCode filterSubject = parseSubjectFilter(requestedSubject);
        String selectedSubjectLabel = filterSubject != null ? filterSubject.name() : "ALL";

        Optional<StudyPlan> planOpt = studyPlanRepository.findByStudentIdAndTargetDate(student.getId(), selectedDate);
        if (planOpt.isEmpty()) {
            return new GuardianDashboardResponse(
                    student.getDisplayName(),
                    student.getGrade(),
                    selectedDate,
                    selectedSubjectLabel,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        StudyPlan plan = planOpt.get();
        List<StudyTask> filteredTasks = plan.getTasks().stream()
                .filter(task -> filterSubject == null || task.getSubject().getCode() == filterSubject)
                .toList();

        List<GuardianTaskItem> items = new ArrayList<>();
        int completedCount = 0;
        int evaluatingCount = 0;

        for (StudyTask task : filteredTasks) {
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

            items.add(new GuardianTaskItem(
                    task.getId(),
                    currentSub != null ? currentSub.getId() : null,
                    plan.getTargetDate(),
                    task.getSubject().getCode(),
                    task.getSubject().getName(),
                    task.getTaskType(),
                    task.getStatus(),
                    currentSub != null ? currentSub.getLessonTitle() : null,
                    task.getRevisionCount(),
                    evalSummary,
                    task.getGuardianAcknowledgedAt(),
                    task.getGuardianNote()
            ));
        }

        return new GuardianDashboardResponse(
                student.getDisplayName(),
                student.getGrade(),
                selectedDate,
                selectedSubjectLabel,
                items.size(),
                completedCount,
                evaluatingCount,
                items
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

    @Transactional(readOnly = true)
    public GuardianWeeklySummaryResponse getWeeklySummary(
            Long guardianUserId,
            LocalDate requestedWeekStart,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = ZoneId.of(resolveTimezone(student));
        LocalDate weekStart = requestedWeekStart != null
                ? requestedWeekStart
                : LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

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
                    dayTotal++;
                    boolean isDone = task.getStatus() == StudyTaskStatus.EVALUATED;
                    if (isDone) {
                        dayCompleted++;
                    }
                    if (task.getGuardianAcknowledgedAt() != null) {
                        dayAcknowledged++;
                    }
                    SubjectCode code = task.getSubject().getCode();
                    subjectNames.putIfAbsent(code, task.getSubject().getName());
                    int[] counts = subjectCounts.computeIfAbsent(code, k -> new int[2]);
                    counts[0]++;
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

    @Transactional(readOnly = true)
    public byte[] exportExcelReport(
            Long guardianUserId,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            Long studentId
    ) {
        StudentProfile student = studentId == null
                ? studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId)
                : studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, studentId);
        ZoneId zone = ZoneId.of(resolveTimezone(student));
        LocalDate today = LocalDate.now(zone);
        LocalDate toDate = requestedTo != null ? requestedTo : today.plusDays(1);
        LocalDate fromDate = requestedFrom != null ? requestedFrom : toDate.minusDays(13);
        if (fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

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

                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(plan.getTargetDate().toString());
                    row.createCell(1).setCellValue(task.getSubject().getName());
                    row.createCell(2).setCellValue(task.getTaskType() == StudyTaskType.REVIEW ? "Ôn bài cũ" : "Soạn bài mới");
                    row.createCell(3).setCellValue(task.getStatus().name());
                    row.createCell(4).setCellValue(currentSub != null ? currentSub.getLessonTitle() : "Chưa nộp");
                    row.createCell(5).setCellValue(eval != null ? eval.getOverallStatus().name() : "-");
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

    private String resolveTimezone(StudentProfile student) {
        return (student.getTimezone() != null && !student.getTimezone().isBlank())
                ? student.getTimezone()
                : appTimezone;
    }
}
