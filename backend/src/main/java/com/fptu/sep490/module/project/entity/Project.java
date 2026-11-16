package com.fptu.sep490.module.project.entity;

import com.fptu.sep490.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Core capstone project workspace entity.
 * Aligned with V1__init_schema.sql and V2__support_multi_repo_github.sql (single-repo columns dropped).
 */
@Entity
@Table(name = "projects")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, length = 30)
    private String projectCode;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "software_type", length = 100)
    private String softwareType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "supervisor_magic_token", nullable = false, unique = true)
    private UUID supervisorMagicToken;

    @Column(name = "supervisor_name", length = 150)
    private String supervisorName;

    @Column(name = "supervisor_email", length = 255)
    private String supervisorEmail;

    @PrePersist
    void ensureSupervisorMagicToken() {
        if (supervisorMagicToken == null) {
            supervisorMagicToken = UUID.randomUUID();
        }
    }
}
