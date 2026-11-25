package com.asc.studycompanion.module.auth;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.dto.LinkCodeResponse;
import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.repository.GuardianLinkRepository;
import com.asc.studycompanion.module.auth.repository.StudentProfileRepository;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private GuardianLinkRepository guardianLinkRepository;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(userRepository, studentProfileRepository, guardianLinkRepository);
    }

    @Test
    void studentGeneratesEightCharCodeAndRevokesPreviousPendingCodes() {
        User studentUser = User.builder().id(1L).role(UserRole.STUDENT).fullName("Nguyen Minh Khoi").build();
        StudentProfile profile = StudentProfile.builder().id(10L).user(studentUser).displayName("Nguyen Minh Khoi").build();
        GuardianLink stale = GuardianLink.builder().id(5L).student(profile).status(GuardianLinkStatus.PENDING).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(guardianLinkRepository.findAllByStudentIdAndStatus(10L, GuardianLinkStatus.PENDING))
                .thenReturn(List.of(stale));
        when(guardianLinkRepository.findByLinkCodeIgnoreCase(any())).thenReturn(Optional.empty());
        when(guardianLinkRepository.save(any(GuardianLink.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkCodeResponse response = linkService.generateLinkCode(1L);

        assertEquals(8, response.linkCode().length());
        assertEquals(GuardianLinkStatus.PENDING, response.status());
        assertEquals(GuardianLinkStatus.REVOKED, stale.getStatus());
        verify(guardianLinkRepository).saveAll(List.of(stale));
    }

    @Test
    void guardianCannotGenerateLinkCodeAndStudentCannotAcceptCode() {
        User guardian = User.builder().id(2L).role(UserRole.GUARDIAN).build();
        User student = User.builder().id(1L).role(UserRole.STUDENT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(guardian));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        AppException genEx = assertThrows(AppException.class, () -> linkService.generateLinkCode(2L));
        assertEquals(ErrorCode.ACCESS_DENIED, genEx.getErrorCode());

        AppException accEx = assertThrows(AppException.class, () -> linkService.acceptLinkCode(1L, "KHOI2026"));
        assertEquals(ErrorCode.ACCESS_DENIED, accEx.getErrorCode());
    }

    @Test
    void guardianAcceptsValidPendingCodeMarksActive() {
        User guardian = User.builder().id(2L).role(UserRole.GUARDIAN).fullName("Nguyen Thu Hang").build();
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Nguyen Minh Khoi").build();
        GuardianLink pending = GuardianLink.builder()
                .id(7L)
                .student(student)
                .linkCode("KHOI2026")
                .status(GuardianLinkStatus.PENDING)
                .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(guardian));
        when(guardianLinkRepository.findByLinkCodeIgnoreCase("KHOI2026")).thenReturn(Optional.of(pending));
        when(guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(2L, 10L, GuardianLinkStatus.ACTIVE))
                .thenReturn(false);
        when(guardianLinkRepository.save(any(GuardianLink.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkCodeResponse response = linkService.acceptLinkCode(2L, "KHOI2026");

        assertEquals(GuardianLinkStatus.ACTIVE, response.status());
        assertEquals("Nguyen Minh Khoi", response.linkedUserName());
        assertNotNull(pending.getLinkedAt());
    }

    @Test
    void acceptRejectsExpiredOrAlreadyLinkedCodes() {
        User guardian = User.builder().id(2L).role(UserRole.GUARDIAN).build();
        StudentProfile student = StudentProfile.builder().id(10L).displayName("Nguyen Minh Khoi").build();

        GuardianLink expired = GuardianLink.builder()
                .student(student)
                .linkCode("EXPIRE00")
                .status(GuardianLinkStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        GuardianLink activeDuplicate = GuardianLink.builder()
                .student(student)
                .linkCode("DUP12345")
                .status(GuardianLinkStatus.PENDING)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(guardian));
        when(guardianLinkRepository.findByLinkCodeIgnoreCase("EXPIRE00")).thenReturn(Optional.of(expired));
        when(guardianLinkRepository.findByLinkCodeIgnoreCase("DUP12345")).thenReturn(Optional.of(activeDuplicate));
        when(guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(2L, 10L, GuardianLinkStatus.ACTIVE))
                .thenReturn(true);

        AppException expiredEx = assertThrows(AppException.class, () -> linkService.acceptLinkCode(2L, "EXPIRE00"));
        assertEquals(ErrorCode.INVALID_LINK_CODE, expiredEx.getErrorCode());

        AppException dupEx = assertThrows(AppException.class, () -> linkService.acceptLinkCode(2L, "DUP12345"));
        assertEquals(ErrorCode.LINK_ALREADY_EXISTS, dupEx.getErrorCode());
    }
}
