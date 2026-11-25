package com.asc.studycompanion.module.auth;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.repository.GuardianLinkRepository;
import com.asc.studycompanion.module.auth.repository.StudentProfileRepository;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyAccessGuardTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private GuardianLinkRepository guardianLinkRepository;

    private StudyAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new StudyAccessGuard(userRepository, studentProfileRepository, guardianLinkRepository);
    }

    @Test
    void studentOwnershipAllowsSelfAndBlocksAnotherStudentId() {
        User studentUser = User.builder().id(1L).role(UserRole.STUDENT).build();
        StudentProfile owned = StudentProfile.builder().id(10L).user(studentUser).displayName("Khoi").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(owned));

        guard.assertStudentOwner(1L, 10L);

        AppException idorEx = assertThrows(AppException.class, () -> guard.assertStudentOwner(1L, 99L));
        assertEquals(ErrorCode.ACCESS_DENIED, idorEx.getErrorCode());
    }

    @Test
    void guardianRequiresActiveLinkAndRejectsUnlinkedStudent() {
        User guardianUser = User.builder().id(2L).role(UserRole.GUARDIAN).build();
        StudentProfile linkedStudent = StudentProfile.builder().id(10L).displayName("Khoi").build();
        GuardianLink activeLink = GuardianLink.builder()
                .id(5L)
                .guardianUser(guardianUser)
                .student(linkedStudent)
                .status(GuardianLinkStatus.ACTIVE)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(guardianLinkRepository.findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(2L, GuardianLinkStatus.ACTIVE))
                .thenReturn(Optional.of(activeLink));
        when(guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(2L, 10L, GuardianLinkStatus.ACTIVE))
                .thenReturn(true);
        when(guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(2L, 88L, GuardianLinkStatus.ACTIVE))
                .thenReturn(false);

        assertEquals(10L, guard.requireLinkedStudentForGuardian(2L).getId());
        guard.assertGuardianCanReadStudent(2L, 10L);

        AppException ex = assertThrows(AppException.class, () -> guard.assertGuardianCanReadStudent(2L, 88L));
        assertEquals(ErrorCode.GUARDIAN_NOT_LINKED, ex.getErrorCode());
    }

    @Test
    void timetableResolutionAllowsBothOwnedStudentAndLinkedGuardian() {
        User studentUser = User.builder().id(1L).role(UserRole.STUDENT).build();
        User guardianUser = User.builder().id(2L).role(UserRole.GUARDIAN).build();
        StudentProfile profile = StudentProfile.builder().id(10L).user(studentUser).grade(8).displayName("Khoi").build();
        GuardianLink activeLink = GuardianLink.builder()
                .id(5L)
                .guardianUser(guardianUser)
                .student(profile)
                .status(GuardianLinkStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(guardianLinkRepository.findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(2L, GuardianLinkStatus.ACTIVE))
                .thenReturn(Optional.of(activeLink));

        assertEquals(10L, guard.resolveStudentProfileForTimetable(1L).getId());
        assertEquals(10L, guard.resolveStudentProfileForTimetable(2L).getId());
    }

    @Test
    void unlinkedGuardianReceivesGrd001Error() {
        User guardianUser = User.builder().id(3L).role(UserRole.GUARDIAN).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(guardianUser));
        when(guardianLinkRepository.findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(3L, GuardianLinkStatus.ACTIVE))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> guard.requireLinkedStudentForGuardian(3L));
        assertEquals(ErrorCode.GUARDIAN_NOT_LINKED, ex.getErrorCode());
    }
}
