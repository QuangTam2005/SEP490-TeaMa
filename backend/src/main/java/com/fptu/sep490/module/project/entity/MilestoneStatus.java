package com.fptu.sep490.module.project.entity;

/**
 * Lifecycle status of a SEP490 milestone.
 * Matches DB check constraint chk_milestones_status.
 */
public enum MilestoneStatus {
    PENDING,
    SUBMITTED,
    APPROVED
}
