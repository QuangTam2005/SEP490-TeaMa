package com.asc.studycompanion.module.auth.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized anti-IDOR authorization guard enforcing Student ownership and active Guardian links.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyAccessGuard {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GuardianLinkRepository guardianLinkRepository;

    public StudentProfile requireOwnedStudentProfile(Long currentUserId) {
        User user = loadUserWithRole(currentUserId, UserRole.STUDENT);
        return studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Resolves StudentProfile for timetable operations, permitting both the owning STUDENT
     * and an actively linked GUARDIAN.
     */
    public StudentProfile resolveStudentProfileForTimetable(Long currentUserId) {
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.STUDENT) {
            return studentProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }
        if (user.getRole() == UserRole.GUARDIAN) {
            return guardianLinkRepository
                    .findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(user.getId(), GuardianLinkStatus.ACTIVE)
                    .map(GuardianLink::getStudent)
                    .orElseThrow(() -> new AppException(ErrorCode.GUARDIAN_NOT_LINKED));
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    public void assertStudentOwner(Long currentUserId, Long targetStudentId) {
        StudentProfile owned = requireOwnedStudentProfile(currentUserId);
        if (!owned.getId().equals(targetStudentId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    public StudentProfile requireLinkedStudentForGuardian(Long guardianUserId) {
        User guardian = loadUserWithRole(guardianUserId, UserRole.GUARDIAN);
        return guardianLinkRepository
                .findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(guardian.getId(), GuardianLinkStatus.ACTIVE)
                .map(GuardianLink::getStudent)
                .orElseThrow(() -> new AppException(ErrorCode.GUARDIAN_NOT_LINKED));
    }

    public void assertGuardianCanReadStudent(Long guardianUserId, Long targetStudentId) {
        User guardian = loadUserWithRole(guardianUserId, UserRole.GUARDIAN);
        boolean linked = guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(
                guardian.getId(),
                targetStudentId,
                GuardianLinkStatus.ACTIVE
        );
        if (!linked) {
            throw new AppException(ErrorCode.GUARDIAN_NOT_LINKED);
        }
    }

    private User loadUserWithRole(Long userId, UserRole expectedRole) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != expectedRole) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return user;
    }
}
