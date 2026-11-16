package com.fptu.sep490.module.project.security;

import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import com.fptu.sep490.module.auth.entity.User;
import com.fptu.sep490.module.project.entity.Project;
import com.fptu.sep490.module.project.entity.ProjectMember;
import com.fptu.sep490.module.project.entity.ProjectRole;
import com.fptu.sep490.module.project.repository.ProjectMemberRepository;
import com.fptu.sep490.module.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessGuard projectAccessGuard;

    private Project activeProject;
    private User memberUser;
    private UUID magicToken;

    @BeforeEach
    void setUp() {
        magicToken = UUID.randomUUID();
        activeProject = Project.builder()
                .id(10L)
                .projectCode("SEP490-G12")
                .projectName("TeaMa Copilot")
                .groupName("G12")
                .startDate(LocalDate.of(2026, 9, 1))
                .supervisorMagicToken(magicToken)
                .build();

        memberUser = User.builder()
                .id(99L)
                .email("student@example.com")
                .passwordHash("hashed")
                .fullName("Nguyen Van A")
                .build();
    }

    @Test
    @DisplayName("requireProjectExists returns project when active project exists")
    void requireProjectExists_ReturnsProject_WhenFound() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));

        Project result = projectAccessGuard.requireProjectExists(10L);

        assertThat(result).isSameAs(activeProject);
    }

    @Test
    @DisplayName("requireProjectExists throws PROJ_001 when project does not exist")
    void requireProjectExists_ThrowsProj001_WhenNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessGuard.requireProjectExists(999L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("requireProjectExists throws PROJ_001 without querying when projectId is null")
    void requireProjectExists_ThrowsProj001_WhenProjectIdNull() {
        assertThatThrownBy(() -> projectAccessGuard.requireProjectExists(null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

        verifyNoInteractions(projectRepository, projectMemberRepository);
    }

    @Test
    @DisplayName("requireMember throws PROJ_001 before checking membership when project is missing")
    void requireMember_ThrowsProj001_WhenProjectMissing() {
        when(projectRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessGuard.requireMember(10L, 99L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    @DisplayName("requireMember throws PROJ_002 when userId is null")
    void requireMember_ThrowsProj002_WhenUserIdNull() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));

        assertThatThrownBy(() -> projectAccessGuard.requireMember(10L, null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);

        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    @DisplayName("requireMember throws PROJ_002 when user is not a member of project")
    void requireMember_ThrowsProj002_WhenNotMember() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));
        when(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessGuard.requireMember(10L, 99L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    @DisplayName("requireMember returns ProjectMember when user belongs to project")
    void requireMember_ReturnsMember_WhenValid() {
        ProjectMember member = ProjectMember.builder()
                .id(1L)
                .project(activeProject)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));
        when(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).thenReturn(Optional.of(member));

        ProjectMember result = projectAccessGuard.requireMember(10L, 99L);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("requireLeader throws PROJ_003 when user is only a MEMBER")
    void requireLeader_ThrowsProj003_WhenRoleIsMember() {
        ProjectMember member = ProjectMember.builder()
                .id(1L)
                .project(activeProject)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));
        when(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> projectAccessGuard.requireLeader(10L, 99L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LEADER_ONLY_ACTION);
    }

    @Test
    @DisplayName("requireLeader returns ProjectMember when user is LEADER")
    void requireLeader_ReturnsMember_WhenRoleIsLeader() {
        ProjectMember leader = ProjectMember.builder()
                .id(1L)
                .project(activeProject)
                .user(memberUser)
                .role(ProjectRole.LEADER)
                .build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(activeProject));
        when(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).thenReturn(Optional.of(leader));

        ProjectMember result = projectAccessGuard.requireLeader(10L, 99L);

        assertThat(result).isSameAs(leader);
    }

    @Test
    @DisplayName("requireProjectByMagicToken returns project when token matches")
    void requireProjectByMagicToken_ReturnsProject_WhenValid() {
        when(projectRepository.findBySupervisorMagicToken(magicToken)).thenReturn(Optional.of(activeProject));

        Project result = projectAccessGuard.requireProjectByMagicToken(magicToken);

        assertThat(result).isSameAs(activeProject);
    }

    @Test
    @DisplayName("requireProjectByMagicToken throws AUTH_006 when token is missing or not found")
    void requireProjectByMagicToken_ThrowsAuth006_WhenInvalid() {
        UUID unknown = UUID.randomUUID();
        when(projectRepository.findBySupervisorMagicToken(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessGuard.requireProjectByMagicToken(unknown))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_MAGIC_TOKEN);

        assertThatThrownBy(() -> projectAccessGuard.requireProjectByMagicToken(null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_MAGIC_TOKEN);
    }
}
