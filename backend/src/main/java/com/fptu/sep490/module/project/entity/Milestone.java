package com.fptu.sep490.module.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SEP490 course milestone entity (e.g. M01..M10) with OGA weighting and due date.
 * Maps directly to milestones in V1__init_schema.sql (no BaseEntity or soft-delete columns).
 */
@Entity
@Table(name = "milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "milestone_code", nullable = false, length = 10)
    private String milestoneCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Builder.Default
    @Column(name = "oga_weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ogaWeightPercent = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MilestoneStatus status = MilestoneStatus.PENDING;
}
