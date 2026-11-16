package com.fptu.sep490.module.project.entity;

import com.fptu.sep490.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

/**
 * GitHub repository configuration linked to a capstone project (multi-repo support from V2).
 * Named ProjectGitRepository to avoid colliding with Spring Data's ProjectRepository interface.
 */
@Entity
@Table(name = "project_repositories")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProjectGitRepository extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "repo_label", nullable = false, length = 50)
    private String repoLabel;

    @Column(name = "repo_full_name", nullable = false, length = 255)
    private String repoFullName;

    @Column(name = "github_repo_url", nullable = false, length = 500)
    private String githubRepoUrl;

    @Column(name = "webhook_secret", nullable = false, length = 255)
    private String webhookSecret;
}
