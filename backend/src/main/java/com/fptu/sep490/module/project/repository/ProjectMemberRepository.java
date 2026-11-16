package com.fptu.sep490.module.project.repository;

import com.fptu.sep490.module.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProjectMember}.
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);

    Optional<ProjectMember> findByIdAndProjectId(Long id, Long projectId);
}
