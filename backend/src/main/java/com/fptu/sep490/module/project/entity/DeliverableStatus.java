package com.fptu.sep490.module.project.entity;

/**
 * Submission status of a milestone deliverable item.
 * Matches DB check constraint chk_milestone_deliverables_status.
 */
public enum DeliverableStatus {
    PENDING,
    SUBMITTED,
    APPROVED
}
