package com.fptu.sep490.module.project.repository;

import com.fptu.sep490.module.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Project}.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySupervisorMagicToken(UUID supervisorMagicToken);

    @Query("""
            SELECT pm.project
            FROM ProjectMember pm
            WHERE pm.user.id = :userId
              AND pm.project.isDeleted = false
            ORDER BY pm.project.createdAt DESC
            """)
    List<Project> findAllByMemberUserId(@Param("userId") Long userId);
}
