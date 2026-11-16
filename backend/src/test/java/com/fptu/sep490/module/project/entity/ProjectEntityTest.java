package com.fptu.sep490.module.project.entity;

import com.fptu.sep490.common.entity.BaseEntity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEntityTest {

    @Test
    @DisplayName("Soft-deletable entities extend BaseEntity and enforce @SQLRestriction('is_deleted = false')")
    void softDeletableEntities_ExtendBaseEntityAndHaveSqlRestriction() {
        assertThat(BaseEntity.class).isAssignableFrom(Project.class);
        assertThat(BaseEntity.class).isAssignableFrom(ProjectGitRepository.class);

        SQLRestriction projectRestriction = Project.class.getAnnotation(SQLRestriction.class);
        SQLRestriction repoRestriction = ProjectGitRepository.class.getAnnotation(SQLRestriction.class);

        assertThat(projectRestriction).isNotNull();
        assertThat(projectRestriction.value()).isEqualTo("is_deleted = false");
        assertThat(repoRestriction).isNotNull();
        assertThat(repoRestriction.value()).isEqualTo("is_deleted = false");
    }

    @Test
    @DisplayName("V1 non-soft-delete tables do not extend BaseEntity or declare @SQLRestriction")
    void nonSoftDeleteEntities_DoNotExtendBaseEntityOrHaveSqlRestriction() {
        for (Class<?> entityClass : new Class<?>[]{
                ProjectMember.class, Milestone.class, MilestoneDeliverable.class}) {
            assertThat(BaseEntity.class.isAssignableFrom(entityClass)).isFalse();
            assertThat(entityClass.getAnnotation(SQLRestriction.class)).isNull();
        }
    }

    @Test
    @DisplayName("Table mappings match V1 and V2 schema names and drop single-repo columns from Project")
    void tableNamesAndV2ProjectFields_MatchFlywaySchema() {
        assertThat(Project.class.getAnnotation(Table.class).name()).isEqualTo("projects");
        assertThat(ProjectGitRepository.class.getAnnotation(Table.class).name()).isEqualTo("project_repositories");
        assertThat(ProjectMember.class.getAnnotation(Table.class).name()).isEqualTo("project_members");
        assertThat(Milestone.class.getAnnotation(Table.class).name()).isEqualTo("milestones");
        assertThat(MilestoneDeliverable.class.getAnnotation(Table.class).name()).isEqualTo("milestone_deliverables");

        Set<String> projectFieldNames = Arrays.stream(Project.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertThat(projectFieldNames).doesNotContain("githubRepoUrl", "webhookSecret");
    }

    @Test
    @DisplayName("Builder defaults, softDelete(), and @PrePersist hooks behave safely")
    void builderDefaultsAndLifecycleHooks_BehaveCorrectly() {
        Project project = Project.builder()
                .projectCode("SEP490-G12")
                .projectName("TeaMa")
                .groupName("G12")
                .startDate(LocalDate.of(2026, 9, 1))
                .build();
        assertThat(project.isDeletedFlag()).isFalse();
        assertThat(project.getSupervisorMagicToken()).isNull();

        project.ensureSupervisorMagicToken();
        UUID generatedToken = project.getSupervisorMagicToken();
        assertThat(generatedToken).isNotNull();
        project.ensureSupervisorMagicToken();
        assertThat(project.getSupervisorMagicToken()).isEqualTo(generatedToken);

        project.softDelete();
        assertThat(project.isDeletedFlag()).isTrue();

        ProjectGitRepository gitRepo = ProjectGitRepository.builder()
                .project(project)
                .repoLabel("Backend")
                .repoFullName("team/be")
                .githubRepoUrl("https://github.com/team/be")
                .webhookSecret("sec-123")
                .build();
        assertThat(gitRepo.isDeletedFlag()).isFalse();
        gitRepo.softDelete();
        assertThat(gitRepo.isDeletedFlag()).isTrue();

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .role(ProjectRole.LEADER)
                .build();
        assertThat(member.getJoinedAt()).isNull();
        member.onPrePersist();
        Instant firstJoinedAt = member.getJoinedAt();
        assertThat(firstJoinedAt).isNotNull();
        member.onPrePersist();
        assertThat(member.getJoinedAt()).isEqualTo(firstJoinedAt);

        Milestone milestone = Milestone.builder()
                .project(project)
                .milestoneCode("M01")
                .name("Project Kickoff")
                .weekNumber(1)
                .build();
        assertThat(milestone.getOgaWeightPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.PENDING);

        MilestoneDeliverable deliverable = MilestoneDeliverable.builder()
                .milestone(milestone)
                .itemName("Project Proposal")
                .build();
        assertThat(deliverable.getIsRequired()).isTrue();
        assertThat(deliverable.getStatus()).isEqualTo(DeliverableStatus.PENDING);
    }
}
