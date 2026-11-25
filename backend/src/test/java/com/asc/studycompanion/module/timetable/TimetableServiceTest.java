package com.asc.studycompanion.module.timetable;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.timetable.dto.TimetableEntryDto;
import com.asc.studycompanion.module.timetable.dto.TimetableResponse;
import com.asc.studycompanion.module.timetable.dto.UpdateTimetableRequest;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import com.asc.studycompanion.module.timetable.repository.TimetableRepository;
import com.asc.studycompanion.module.timetable.service.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private TimetableRepository timetableRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;

    private TimetableService timetableService;

    @BeforeEach
    void setUp() {
        timetableService = new TimetableService(
                studyAccessGuard,
                timetableRepository,
                subjectRepository,
                userRepository
        );
    }

    @Test
    void returnsEmptySnapshotWhenNoTimetableSavedYet() {
        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .displayName("Nguyen Minh Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        when(studyAccessGuard.resolveStudentProfileForTimetable(1L)).thenReturn(profile);
        when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(10L, TimetableStatus.ACTIVE))
                .thenReturn(Optional.empty());

        TimetableResponse response = timetableService.getTimetable(1L);

        assertEquals(0, response.version());
        assertEquals("Nguyen Minh Khoi", response.updatedByName());
        assertTrue(response.entries().isEmpty());
    }

    @Test
    void guardianUpdateCreatesNewVersionAndArchivesPreviousActiveVersion() {
        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .displayName("Nguyen Minh Khoi")
                .grade(7)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        User guardian = User.builder()
                .id(2L)
                .fullName("Nguyen Thu Hang")
                .role(UserRole.GUARDIAN)
                .build();
        Timetable previous = Timetable.builder()
                .id(5L)
                .student(profile)
                .version(2)
                .status(TimetableStatus.ACTIVE)
                .effectiveDate(LocalDate.of(2026, 11, 20))
                .build();

        Subject math = Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").isActive(true).build();
        Subject lit = Subject.builder().id(2L).code(SubjectCode.LIT).name("Ngữ văn").isActive(true).build();

        when(studyAccessGuard.resolveStudentProfileForTimetable(2L)).thenReturn(profile);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardian));
        when(subjectRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(math, lit));
        when(timetableRepository.findTopByStudentIdOrderByVersionDesc(10L)).thenReturn(Optional.of(previous));
        when(timetableRepository.findAllByStudentIdAndStatus(10L, TimetableStatus.ACTIVE)).thenReturn(List.of(previous));
        when(timetableRepository.save(any(Timetable.class))).thenAnswer(inv -> {
            Timetable t = inv.getArgument(0);
            t.setId(6L);
            t.setUpdatedAt(Instant.parse("2026-11-25T03:00:00Z"));
            return t;
        });

        UpdateTimetableRequest request = new UpdateTimetableRequest(List.of(
                new TimetableEntryDto(1, SubjectCode.MATH, "Tiet 1"),
                new TimetableEntryDto(3, SubjectCode.LIT, " ")
        ));

        TimetableResponse saved = timetableService.updateTimetable(2L, request);

        assertEquals(TimetableStatus.ARCHIVED, previous.getStatus());
        verify(timetableRepository).saveAll(List.of(previous));

        ArgumentCaptor<Timetable> captor = ArgumentCaptor.forClass(Timetable.class);
        verify(timetableRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals(TimetableStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(guardian, captor.getValue().getUpdatedBy());

        assertEquals(3, saved.version());
        assertEquals("Nguyen Thu Hang", saved.updatedByName());
        assertEquals(2, saved.entries().size());
        assertEquals("Tiet 1", saved.entries().get(0).periodNote());
        assertEquals(null, saved.entries().get(1).periodNote());
    }

    @Test
    void unlinkedGuardianRejectedWithGrd001() {
        when(studyAccessGuard.resolveStudentProfileForTimetable(99L))
                .thenThrow(new AppException(ErrorCode.GUARDIAN_NOT_LINKED));

        AppException ex = assertThrows(
                AppException.class,
                () -> timetableService.getTimetable(99L)
        );
        assertEquals(ErrorCode.GUARDIAN_NOT_LINKED, ex.getErrorCode());
    }
}
