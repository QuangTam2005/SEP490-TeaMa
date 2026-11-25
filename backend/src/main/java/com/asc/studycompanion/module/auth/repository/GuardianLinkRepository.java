package com.asc.studycompanion.module.auth.repository;

import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, Long> {

    Optional<GuardianLink> findByLinkCodeIgnoreCase(String linkCode);

    Optional<GuardianLink> findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(
            Long guardianUserId,
            GuardianLinkStatus status
    );

    Optional<GuardianLink> findFirstByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<GuardianLink> findAllByStudentIdAndStatus(Long studentId, GuardianLinkStatus status);

    boolean existsByGuardianUserIdAndStudentIdAndStatus(
            Long guardianUserId,
            Long studentId,
            GuardianLinkStatus status
    );
}
