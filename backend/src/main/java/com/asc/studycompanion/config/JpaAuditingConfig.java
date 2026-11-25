package com.asc.studycompanion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA Auditing so @CreatedDate and @LastModifiedDate
 * timestamps are populated automatically on entity persistence and updates.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
