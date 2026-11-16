package com.fptu.sep490.module.project.security;

import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import com.fptu.sep490.module.project.entity.Project;
import com.fptu.sep490.module.project.entity.ProjectMember;
import com.fptu.sep490.module.project.entity.ProjectRole;
import com.fptu.sep490.module.project.repository.ProjectMemberRepository;
import com.fptu.sep490.module.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Project-Scope authorization & anti-IDOR guard shared across all project-scoped modules.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAccessGuard {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Ensures the target active (non-deleted) project exists.
     */
    public Project requireProjectExists(Long projectId) {
        if (projectId == null) {
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
    }

    /**
     * Ensures the project exists and the given user is a member of it.
     * Checks project existence first so missing/deleted projects fail with 404 PROJ_001 rather than 403.
     */
    public ProjectMember requireMember(Long projectId, Long userId) {
        requireProjectExists(projectId);
        if (userId == null) {
            throw new AppException(ErrorCode.NOT_PROJECT_MEMBER);
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    /**
     * Ensures the user is an active LEADER of the target project.
     */
    public ProjectMember requireLeader(Long projectId, Long userId) {
        ProjectMember member = requireMember(projectId, userId);
        if (member.getRole() != ProjectRole.LEADER) {
            throw new AppException(ErrorCode.LEADER_ONLY_ACTION);
        }
        return member;
    }

    /**
     * Resolves an active project from a Supervisor read-only Magic Link token.
     */
    public Project requireProjectByMagicToken(UUID magicToken) {
        if (magicToken == null) {
            throw new AppException(ErrorCode.INVALID_MAGIC_TOKEN);
        }
        return projectRepository.findBySupervisorMagicToken(magicToken)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MAGIC_TOKEN));
    }
}
