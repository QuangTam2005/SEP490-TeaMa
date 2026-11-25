package com.asc.studycompanion.module.auth.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final Duration CODE_TTL = Duration.ofHours(24);

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GuardianLinkRepository guardianLinkRepository;

    @Transactional(readOnly = true)
    public LinkCodeResponse getCurrentLink(Long userId) {
        User user = loadUser(userId);
        if (user.getRole() == UserRole.STUDENT) {
            StudentProfile student = loadStudentProfile(user.getId());
            return guardianLinkRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId())
                    .map(link -> new LinkCodeResponse(
                            link.getLinkCode(),
                            link.getExpiresAt(),
                            link.getStatus(),
                            link.getGuardianUser() != null ? link.getGuardianUser().getFullName() : null
                    ))
                    .orElse(null);
        }

        return guardianLinkRepository
                .findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(user.getId(), GuardianLinkStatus.ACTIVE)
                .map(link -> new LinkCodeResponse(
                        link.getLinkCode(),
                        link.getExpiresAt(),
                        link.getStatus(),
                        link.getStudent().getDisplayName()
                ))
                .orElse(null);
    }

    @Transactional
    public LinkCodeResponse generateLinkCode(Long studentUserId) {
        User user = loadUser(studentUserId);
        if (user.getRole() != UserRole.STUDENT) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        StudentProfile student = loadStudentProfile(user.getId());
        List<GuardianLink> stalePending = guardianLinkRepository
                .findAllByStudentIdAndStatus(student.getId(), GuardianLinkStatus.PENDING);
        for (GuardianLink pending : stalePending) {
            pending.setStatus(GuardianLinkStatus.REVOKED);
        }
        if (!stalePending.isEmpty()) {
            guardianLinkRepository.saveAll(stalePending);
        }

        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plus(CODE_TTL);
        GuardianLink created = GuardianLink.builder()
                .student(student)
                .linkCode(code)
                .status(GuardianLinkStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        GuardianLink saved = guardianLinkRepository.save(created);
        return new LinkCodeResponse(
                saved.getLinkCode(),
                saved.getExpiresAt(),
                saved.getStatus(),
                null
        );
    }

    @Transactional
    public LinkCodeResponse acceptLinkCode(Long guardianUserId, String rawLinkCode) {
        User guardian = loadUser(guardianUserId);
        if (guardian.getRole() != UserRole.GUARDIAN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        GuardianLink link = guardianLinkRepository.findByLinkCodeIgnoreCase(rawLinkCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_LINK_CODE));

        if (link.getStatus() != GuardianLinkStatus.PENDING || link.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.INVALID_LINK_CODE);
        }

        StudentProfile student = link.getStudent();
        if (guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(
                guardian.getId(),
                student.getId(),
                GuardianLinkStatus.ACTIVE
        )) {
            throw new AppException(ErrorCode.LINK_ALREADY_EXISTS);
        }

        link.setGuardianUser(guardian);
        link.setStatus(GuardianLinkStatus.ACTIVE);
        link.setLinkedAt(Instant.now());
        GuardianLink saved = guardianLinkRepository.save(link);

        return new LinkCodeResponse(
                saved.getLinkCode(),
                saved.getExpiresAt(),
                saved.getStatus(),
                student.getDisplayName()
        );
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private StudentProfile loadStudentProfile(Long userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (guardianLinkRepository.findByLinkCodeIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate unique 8-char link code");
    }
}
