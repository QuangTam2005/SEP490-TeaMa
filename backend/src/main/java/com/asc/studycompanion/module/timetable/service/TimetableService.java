package com.asc.studycompanion.module.timetable.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.timetable.dto.TimetableEntryDto;
import com.asc.studycompanion.module.timetable.dto.TimetableResponse;
import com.asc.studycompanion.module.timetable.dto.UpdateTimetableRequest;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableEntry;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import com.asc.studycompanion.module.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final StudyAccessGuard studyAccessGuard;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone = "Asia/Ho_Chi_Minh";

    @Transactional(readOnly = true)
    public TimetableResponse getTimetable(Long currentUserId) {
        StudentProfile student = studyAccessGuard.resolveStudentProfileForTimetable(currentUserId);
        return timetableRepository
                .findFirstByStudentIdAndStatusOrderByVersionDesc(student.getId(), TimetableStatus.ACTIVE)
                .map(this::toResponse)
                .orElseGet(() -> new TimetableResponse(
                        null,
                        0,
                        LocalDate.now(ZoneId.of(resolveTimezone(student))),
                        Instant.now(),
                        student.getDisplayName(),
                        List.of()
                ));
    }

    @Transactional
    public TimetableResponse updateTimetable(Long currentUserId, UpdateTimetableRequest request) {
        StudentProfile student = studyAccessGuard.resolveStudentProfileForTimetable(currentUserId);
        User actor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Map<SubjectCode, Subject> subjectByCode = subjectRepository.findAllByIsActiveTrueOrderByIdAsc()
                .stream()
                .collect(Collectors.toMap(Subject::getCode, Function.identity()));

        int nextVersion = timetableRepository.findTopByStudentIdOrderByVersionDesc(student.getId())
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);

        // Archive previous ACTIVE versions without touching historical plans or submissions (BR-20)
        List<Timetable> previousActives = timetableRepository
                .findAllByStudentIdAndStatus(student.getId(), TimetableStatus.ACTIVE);
        for (Timetable prev : previousActives) {
            prev.setStatus(TimetableStatus.ARCHIVED);
        }
        if (!previousActives.isEmpty()) {
            timetableRepository.saveAll(previousActives);
        }

        Timetable nextTimetable = Timetable.builder()
                .student(student)
                .effectiveDate(LocalDate.now(ZoneId.of(resolveTimezone(student))))
                .version(nextVersion)
                .status(TimetableStatus.ACTIVE)
                .updatedBy(actor)
                .build();

        for (TimetableEntryDto entryDto : request.entries()) {
            Subject subject = subjectByCode.get(entryDto.subjectCode());
            if (subject == null) {
                throw new AppException(ErrorCode.INVALID_SUBJECT_CODE);
            }
            nextTimetable.getEntries().add(TimetableEntry.builder()
                    .timetable(nextTimetable)
                    .weekday(entryDto.weekday())
                    .subject(subject)
                    .periodNote(entryDto.periodNote())
                    .build());
        }

        Timetable saved = timetableRepository.save(nextTimetable);
        return toResponse(saved);
    }

    private TimetableResponse toResponse(Timetable timetable) {
        List<TimetableEntryDto> entries = timetable.getEntries()
                .stream()
                .map(entry -> new TimetableEntryDto(
                        entry.getWeekday(),
                        entry.getSubject().getCode(),
                        entry.getPeriodNote()
                ))
                .toList();

        return new TimetableResponse(
                timetable.getId(),
                timetable.getVersion(),
                timetable.getEffectiveDate(),
                timetable.getUpdatedAt(),
                timetable.getUpdatedBy().getFullName(),
                entries
        );
    }

    private String resolveTimezone(StudentProfile student) {
        return (student.getTimezone() != null && !student.getTimezone().isBlank())
                ? student.getTimezone()
                : appTimezone;
    }
}
