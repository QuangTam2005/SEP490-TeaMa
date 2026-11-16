package com.fptu.sep490.module.project.repository;

import com.fptu.sep490.module.project.entity.MilestoneDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MilestoneDeliverable}.
 */
public interface MilestoneDeliverableRepository extends JpaRepository<MilestoneDeliverable, Long> {

    List<MilestoneDeliverable> findAllByMilestoneIdOrderByIdAsc(Long milestoneId);

    @Query("""
            SELECT d
            FROM MilestoneDeliverable d
            JOIN FETCH d.milestone m
            WHERE d.id = :id
              AND m.project.id = :projectId
            """)
    Optional<MilestoneDeliverable> findByIdAndProjectId(
            @Param("id") Long id,
            @Param("projectId") Long projectId
    );
}
