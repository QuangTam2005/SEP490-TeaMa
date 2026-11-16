package com.fptu.sep490.module.project.repository;

import com.fptu.sep490.module.project.entity.ProjectGitRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProjectGitRepository} (project_repositories table).
 */
public interface ProjectGitRepositoryRepository extends JpaRepository<ProjectGitRepository, Long> {

    List<ProjectGitRepository> findAllByProjectIdOrderByIdAsc(Long projectId);

    Optional<ProjectGitRepository> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndRepoFullNameIgnoreCase(Long projectId, String repoFullName);
}
