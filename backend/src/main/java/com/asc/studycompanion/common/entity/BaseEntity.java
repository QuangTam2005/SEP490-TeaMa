package com.asc.studycompanion.common.entity;

import com.asc.studycompanion.common.util.SecurityAuditorHelper;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base mapped superclass for business entities supporting user auditing and soft-delete.
 * Note: Concrete soft-deletable @Entity classes must explicitly declare
 * @SQLRestriction("is_deleted = false") to comply with Hibernate 6.4+ rules.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseEntity extends BaseTimestampEntity {

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @PrePersist
    protected void onPrePersistAudit() {
        if (this.isDeleted == null) {
            this.isDeleted = Boolean.FALSE;
        }
        SecurityAuditorHelper.getCurrentUserId().ifPresent(userId -> {
            if (this.createdBy == null) {
                this.createdBy = userId;
            }
            if (this.updatedBy == null) {
                this.updatedBy = userId;
            }
        });
    }

    @PreUpdate
    protected void onPreUpdateAudit() {
        // Only overwrite updatedBy when an authenticated user triggers the update.
        // Background/webhook updates (e.g., GitHub PR sync) preserve the previous updatedBy.
        SecurityAuditorHelper.getCurrentUserId().ifPresent(userId -> this.updatedBy = userId);
    }

    public void softDelete() {
        this.isDeleted = Boolean.TRUE;
    }

    public boolean isDeletedFlag() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
