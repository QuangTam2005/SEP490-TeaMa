package com.asc.studycompanion.module.study.entity;

import com.asc.studycompanion.common.entity.BaseTimestampEntity;
import com.asc.studycompanion.module.timetable.entity.Subject;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Single REVIEW or PREVIEW learning task for a subject in a StudyPlan.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "study_tasks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_study_tasks_plan_subject_type",
                columnNames = {"plan_id", "subject_id", "task_type"}
        )
)
public class StudyTask extends BaseTimestampEntity {

    public static final int MAX_REVISIONS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private StudyTaskType taskType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StudyTaskStatus status = StudyTaskStatus.NOT_STARTED;

    @Builder.Default
    @Column(name = "revision_count", nullable = false)
    private Integer revisionCount = 0;

    @Column(name = "guardian_acknowledged_at")
    private java.time.Instant guardianAcknowledgedAt;

    @Column(name = "guardian_note", length = 300)
    private String guardianNote;

    @Column(name = "guardian_acknowledged_by_user_id")
    private Long guardianAcknowledgedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_request_status", length = 20)
    private SkipRequestStatus skipRequestStatus;

    @Column(name = "skip_request_reason", length = 300)
    private String skipRequestReason;

    @Column(name = "skip_requested_at")
    private java.time.Instant skipRequestedAt;

    @Column(name = "skip_reviewed_at")
    private java.time.Instant skipReviewedAt;

    @Column(name = "skip_reviewed_by_user_id")
    private Long skipReviewedByUserId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode", nullable = false, length = 30)
    private TaskAssignmentMode assignmentMode = TaskAssignmentMode.STANDARD_AI;

    @Column(name = "custom_task_title", length = 200)
    private String customTaskTitle;

    @Column(name = "custom_task_description", length = 500)
    private String customTaskDescription;

    @Column(name = "custom_drive_folder_url", length = 500)
    private String customDriveFolderUrl;

    @Column(name = "custom_assigned_by_user_id")
    private Long customAssignedByUserId;

    @Column(name = "custom_assigned_at")
    private java.time.Instant customAssignedAt;

    @Column(name = "custom_submission_note", length = 300)
    private String customSubmissionNote;

    @Column(name = "custom_submission_url", length = 500)
    private String customSubmissionUrl;

    @Column(name = "custom_submitted_at")
    private java.time.Instant customSubmittedAt;
}
