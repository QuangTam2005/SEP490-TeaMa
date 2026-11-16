package com.fptu.sep490.module.project.repository;

import com.fptu.sep490.module.project.entity.Milestone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Milestone}.
 */
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findAllByProjectIdOrderByWeekNumberAsc(Long projectId);

    @EntityGraph(attributePaths = {"project"})
    Optional<Milestone> findByIdAndProjectId(Long id, Long projectId);
}
