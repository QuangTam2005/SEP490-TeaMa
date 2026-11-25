package com.asc.studycompanion.module.study.service;

import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.dto.NextDayStudyPlanResponse;
import com.asc.studycompanion.module.study.dto.NextDaySubjectPair;
import com.asc.studycompanion.module.study.dto.StudentStreakResponse;
import com.asc.studycompanion.module.study.dto.StudyPlanHistoryItem;
import com.asc.studycompanion.module.study.dto.StudyTaskSummaryResponse;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableEntry;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.asc.studycompanion.module.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private static final List<String> WEEKDAY_LABELS = List.of(
            "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"
    );

    private final StudyAccessGuard studyAccessGuard;
    private final StudyPlanRepository studyPlanRepository;
    private final TimetableRepository timetableRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final AIEvaluationRepository aiEvaluationRepository;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone = "Asia/Ho_Chi_Minh";

    private Clock clock = Clock.systemUTC();

    /**
     * Package/test helper to inject deterministic Clock in unit tests.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public NextDayStudyPlanResponse getNextDayPlan(Long studentUserId) {
        StudentProfile student = studyAccessGuard.requireOwnedStudentProfile(studentUserId);
        ZoneId zoneId = ZoneId.of(resolveTimezone(student));
        LocalDate targetDate = LocalDate.now(clock.withZone(zoneId)).plusDays(1);
        int targetWeekday = targetDate.getDayOfWeek().getValue();
        String weekdayLabel = WEEKDAY_LABELS.get(targetWeekday - 1);

        Optional<Timetable> activeTimetableOpt = timetableRepository
                .findFirstByStudentIdAndStatusOrderByVersionDesc(student.getId(), TimetableStatus.ACTIVE);

        Optional<StudyPlan> existingPlanOpt = studyPlanRepository
                .findByStudentIdAndTargetDate(student.getId(), targetDate);

        if (existingPlanOpt.isPresent()) {
            StudyPlan existingPlan = existingPlanOpt.get();
            if (activeTimetableOpt.isPresent()
                    && canRefreshUnstartedPlan(existingPlan, activeTimetableOpt.get())) {
                return rebuildUnstartedPlan(existingPlan, activeTimetableOpt.get(), targetWeekday, weekdayLabel);
            }
            return toPlanResponse(existingPlan, targetDate, weekdayLabel, targetWeekday);
        }

        if (activeTimetableOpt.isEmpty()) {
            return new NextDayStudyPlanResponse(null, targetDate, weekdayLabel, false, List.of());
        }

        Timetable activeTimetable = activeTimetableOpt.get();
        Map<SubjectCode, SubjectSlot> subjectSlots = extractDistinctSubjectsForWeekday(activeTimetable, targetWeekday);
        if (subjectSlots.isEmpty()) {
            return new NextDayStudyPlanResponse(null, targetDate, weekdayLabel, false, List.of());
        }

        StudyPlan newPlan = StudyPlan.builder()
                .student(student)
                .targetDate(targetDate)
                .timetable(activeTimetable)
                .build();

        for (SubjectSlot slot : subjectSlots.values()) {
            newPlan.getTasks().add(StudyTask.builder()
                    .plan(newPlan)
                    .subject(slot.subject())
                    .taskType(StudyTaskType.REVIEW)
                    .status(StudyTaskStatus.NOT_STARTED)
                    .revisionCount(0)
                    .build());
            newPlan.getTasks().add(StudyTask.builder()
                    .plan(newPlan)
                    .subject(slot.subject())
                    .taskType(StudyTaskType.PREVIEW)
                    .status(StudyTaskStatus.BLOCKED)
                    .revisionCount(0)
                    .build());
        }

        StudyPlan savedPlan;
        try {
            savedPlan = studyPlanRepository.saveAndFlush(newPlan);
        } catch (DataIntegrityViolationException ex) {
            savedPlan = studyPlanRepository.findByStudentIdAndTargetDate(student.getId(), targetDate)
                    .orElseThrow(() -> ex);
        }

        return toPlanResponse(savedPlan, targetDate, weekdayLabel, targetWeekday);
    }

    private boolean canRefreshUnstartedPlan(StudyPlan plan, Timetable activeTimetable) {
        boolean timetableChanged = plan.getTimetable() == null
                || !plan.getTimetable().getId().equals(activeTimetable.getId());
        if (!timetableChanged) {
            return false;
        }
        return plan.getTasks().stream().allMatch(task ->
                task.getStatus() == StudyTaskStatus.NOT_STARTED
                        || task.getStatus() == StudyTaskStatus.BLOCKED
        );
    }

    private NextDayStudyPlanResponse rebuildUnstartedPlan(
            StudyPlan plan,
            Timetable activeTimetable,
            int targetWeekday,
            String weekdayLabel
    ) {
        Map<SubjectCode, SubjectSlot> subjectSlots = extractDistinctSubjectsForWeekday(activeTimetable, targetWeekday);
        plan.setTimetable(activeTimetable);
        plan.getTasks().clear();

        if (subjectSlots.isEmpty()) {
            studyPlanRepository.delete(plan);
            return new NextDayStudyPlanResponse(null, plan.getTargetDate(), weekdayLabel, false, List.of());
        }

        for (SubjectSlot slot : subjectSlots.values()) {
            plan.getTasks().add(StudyTask.builder()
                    .plan(plan)
                    .subject(slot.subject())
                    .taskType(StudyTaskType.REVIEW)
                    .status(StudyTaskStatus.NOT_STARTED)
                    .revisionCount(0)
                    .build());
            plan.getTasks().add(StudyTask.builder()
                    .plan(plan)
                    .subject(slot.subject())
                    .taskType(StudyTaskType.PREVIEW)
                    .status(StudyTaskStatus.BLOCKED)
                    .revisionCount(0)
                    .build());
        }

        StudyPlan saved = studyPlanRepository.save(plan);
        return toPlanResponse(saved, saved.getTargetDate(), weekdayLabel, targetWeekday);
    }

    @Transactional(readOnly = true)
    public List<StudyPlanHistoryItem> getPlanHistory(Long studentUserId, LocalDate fromDate, LocalDate toDate) {
        StudentProfile student = studyAccessGuard.requireOwnedStudentProfile(studentUserId);
        List<StudyPlan> plans = (fromDate != null && toDate != null)
                ? studyPlanRepository.findAllByStudentIdAndTargetDateBetweenOrderByTargetDateDesc(
                        student.getId(), fromDate, toDate)
                : studyPlanRepository.findAllByStudentIdOrderByTargetDateDesc(student.getId());

        return plans.stream()
                .filter(plan -> !plan.getTasks().isEmpty())
                .limit(30)
                .map(plan -> {
                    int weekday = plan.getTargetDate().getDayOfWeek().getValue();
                    String weekdayLabel = WEEKDAY_LABELS.get(weekday - 1);
                    NextDayStudyPlanResponse planDto = toPlanResponse(plan, plan.getTargetDate(), weekdayLabel, weekday);
                    int totalTasks = plan.getTasks().size();
                    int completedTasks = (int) plan.getTasks().stream()
                            .filter(t -> t.getStatus() == StudyTaskStatus.EVALUATED)
                            .count();
                    return new StudyPlanHistoryItem(
                            plan.getId(),
                            plan.getTargetDate(),
                            weekdayLabel,
                            totalTasks,
                            completedTasks,
                            planDto.subjects()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentStreakResponse getStreak(Long studentUserId) {
        StudentProfile student = studyAccessGuard.requireOwnedStudentProfile(studentUserId);
        ZoneId zoneId = ZoneId.of(resolveTimezone(student));
        LocalDate today = LocalDate.now(clock.withZone(zoneId));
        LocalDate tomorrow = today.plusDays(1);

        List<LocalDate> completedDates = studyPlanRepository.findAllByStudentIdOrderByTargetDateDesc(student.getId())
                .stream()
                .filter(plan -> plan.getTasks().stream()
                        .anyMatch(task -> task.getStatus() == StudyTaskStatus.EVALUATED))
                .map(StudyPlan::getTargetDate)
                .distinct()
                .sorted()
                .toList();

        if (completedDates.isEmpty()) {
            return new StudentStreakResponse(0, 0, 0, null);
        }

        int longestStreak = 1;
        int running = 1;
        for (int i = 1; i < completedDates.size(); i++) {
            if (completedDates.get(i).equals(completedDates.get(i - 1).plusDays(1))) {
                running++;
                if (running > longestStreak) {
                    longestStreak = running;
                }
            } else {
                running = 1;
            }
        }

        java.util.Set<LocalDate> dateSet = new java.util.HashSet<>(completedDates);
        LocalDate anchor = dateSet.contains(tomorrow)
                ? tomorrow
                : (dateSet.contains(today) ? today : null);

        int currentStreak = 0;
        if (anchor != null) {
            LocalDate cursor = anchor;
            while (dateSet.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        }

        return new StudentStreakResponse(
                currentStreak,
                longestStreak,
                completedDates.size(),
                completedDates.get(completedDates.size() - 1)
        );
    }

    private NextDayStudyPlanResponse toPlanResponse(
            StudyPlan plan,
            LocalDate targetDate,
            String weekdayLabel,
            int targetWeekday
    ) {
        if (plan.getTasks().isEmpty()) {
            return new NextDayStudyPlanResponse(plan.getId(), targetDate, weekdayLabel, false, List.of());
        }

        Map<SubjectCode, String> notesBySubject = plan.getTimetable() != null
                ? extractDistinctSubjectsForWeekday(plan.getTimetable(), targetWeekday)
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().periodNote() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().periodNote()))
                : Map.of();

        Map<SubjectCode, List<StudyTask>> grouped = new LinkedHashMap<>();
        for (StudyTask task : plan.getTasks()) {
            grouped.computeIfAbsent(task.getSubject().getCode(), k -> new ArrayList<>()).add(task);
        }

        List<NextDaySubjectPair> pairs = new ArrayList<>();
        for (Map.Entry<SubjectCode, List<StudyTask>> entry : grouped.entrySet()) {
            List<StudyTask> tasks = entry.getValue();
            StudyTask review = tasks.stream()
                    .filter(t -> t.getTaskType() == StudyTaskType.REVIEW)
                    .findFirst()
                    .orElse(null);
            StudyTask preview = tasks.stream()
                    .filter(t -> t.getTaskType() == StudyTaskType.PREVIEW)
                    .findFirst()
                    .orElse(null);
            if (review != null && preview != null) {
                pairs.add(new NextDaySubjectPair(
                        entry.getKey(),
                        review.getSubject().getName(),
                        notesBySubject.get(entry.getKey()),
                        toSummary(review),
                        toSummary(preview)
                ));
            }
        }

        return new NextDayStudyPlanResponse(
                plan.getId(),
                targetDate,
                weekdayLabel,
                !pairs.isEmpty(),
                pairs
        );
    }

    private StudyTaskSummaryResponse toSummary(StudyTask task) {
        String currentLessonTitle = null;
        OverallEvaluationStatus latestEvaluationStatus = null;
        if (task.getId() != null) {
            Optional<StudySubmission> currentSubmission = studySubmissionRepository
                    .findFirstByTaskIdAndIsCurrentTrue(task.getId());
            if (currentSubmission.isPresent()) {
                StudySubmission submission = currentSubmission.get();
                currentLessonTitle = submission.getLessonTitle();
                if (submission.getId() != null) {
                    latestEvaluationStatus = aiEvaluationRepository.findBySubmissionId(submission.getId())
                            .map(AIEvaluation::getOverallStatus)
                            .orElse(null);
                }
            }
        }
        return new StudyTaskSummaryResponse(
                task.getId(),
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                task.getRevisionCount(),
                StudyTask.MAX_REVISIONS,
                currentLessonTitle,
                latestEvaluationStatus,
                task.getGuardianAcknowledgedAt(),
                task.getGuardianNote()
        );
    }

    private Map<SubjectCode, SubjectSlot> extractDistinctSubjectsForWeekday(
            Timetable timetable,
            int weekday
    ) {
        Map<SubjectCode, SubjectSlot> slots = new LinkedHashMap<>();
        for (TimetableEntry entry : timetable.getEntries()) {
            if (!Integer.valueOf(weekday).equals(entry.getWeekday())) {
                continue;
            }
            Subject subject = entry.getSubject();
            String note = (entry.getPeriodNote() != null && !entry.getPeriodNote().isBlank())
                    ? entry.getPeriodNote().trim()
                    : null;

            slots.merge(
                    subject.getCode(),
                    new SubjectSlot(subject, note),
                    (existing, incoming) -> {
                        if (incoming.periodNote() == null) {
                            return existing;
                        }
                        if (existing.periodNote() == null) {
                            return new SubjectSlot(existing.subject(), incoming.periodNote());
                        }
                        return new SubjectSlot(
                                existing.subject(),
                                existing.periodNote() + ", " + incoming.periodNote()
                        );
                    }
            );
        }
        return slots;
    }

    private String resolveTimezone(StudentProfile student) {
        return (student.getTimezone() != null && !student.getTimezone().isBlank())
                ? student.getTimezone()
                : appTimezone;
    }

    private record SubjectSlot(Subject subject, String periodNote) {
    }
}
