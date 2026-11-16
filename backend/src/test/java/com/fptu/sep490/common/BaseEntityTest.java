package com.fptu.sep490.common;

import com.fptu.sep490.common.entity.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    static class SampleBusinessEntity extends BaseEntity {
        private String code;

        void triggerPrePersist() {
            onPrePersistAudit();
        }

        void triggerPreUpdate() {
            onPreUpdateAudit();
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superBuilderInitializesIsDeletedToFalseWithoutNpe() {
        SampleBusinessEntity entity = SampleBusinessEntity.builder()
                .code("FE-01")
                .build();

        assertNotNull(entity.getIsDeleted());
        assertFalse(entity.getIsDeleted());
        assertFalse(entity.isDeletedFlag());

        entity.softDelete();
        assertTrue(entity.isDeletedFlag());
    }

    @Test
    void prePersistAndPreUpdateAuditPreserveUpdatedByOnWebhookOrAnonymousUpdate() {
        // User 10 creates entity
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, Collections.emptyList())
        );
        SampleBusinessEntity entity = SampleBusinessEntity.builder()
                .code("TASK-01")
                .isDeleted(null)
                .build();
        entity.triggerPrePersist();

        assertFalse(entity.getIsDeleted());
        assertEquals(10L, entity.getCreatedBy());
        assertEquals(10L, entity.getUpdatedBy());

        // Background GitHub Webhook updates entity without authenticated user
        SecurityContextHolder.clearContext();
        entity.triggerPreUpdate();

        assertEquals(10L, entity.getUpdatedBy(), "Webhook update must preserve previous updatedBy");

        // User 25 updates entity later
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("25", null, Collections.emptyList())
        );
        entity.triggerPreUpdate();

        assertEquals(10L, entity.getCreatedBy());
        assertEquals(25L, entity.getUpdatedBy());
    }
}
