package com.asc.studycompanion.module.guardian.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.guardian.dto.GuardianDashboardResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianEvaluationSummary;
import com.asc.studycompanion.module.guardian.dto.GuardianSubmissionDetailResponse;
import com.asc.studycompanion.module.guardian.dto.GuardianTaskItem;
import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final StudyAccessGuard studyAccessGuard;
    private final StudyPlanRepository studyPlanRepository;
    private final StudySubmissionRepository studySubmissionRepository;
    private final AIEvaluationRepository aiEvaluationRepository;
    private final EvaluationJobRepository evaluationJobRepository;
    private final StudyTaskService studyTaskService;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone = "Asia/Ho_Chi_Minh";

    @Transactional(readOnly = true)
    public GuardianDashboardResponse getDashboard(
            Long guardianUserId,
            LocalDate requestedDate,
            String requestedSubject
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId);
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
                    evalSummary
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
                student.getDisplayName(),
                task.getPlan().getTargetDate(),
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                task.getRevisionCount(),
                versionDtos
        );
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
