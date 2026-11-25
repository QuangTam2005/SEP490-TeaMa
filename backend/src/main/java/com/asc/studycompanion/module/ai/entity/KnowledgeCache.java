package com.asc.studycompanion.module.ai.entity;

import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Curriculum web source cache mapped to knowledge_cache (created_at + expires_at, policy-gated).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_cache")
public class KnowledgeCache {

    public static final String CURRENT_POLICY_VERSION = "v3";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true, length = 255)
    private String cacheKey;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Column(name = "subject_code", nullable = false, length = 20)
    private String subjectCode;

    @Column(name = "normalized_lesson_title", nullable = false, length = 255)
    private String normalizedLessonTitle;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources_json", nullable = false, columnDefinition = "jsonb")
    private List<SearchSnippet> sourcesJson = new ArrayList<>();

    @Builder.Default
    @Column(name = "cache_allowed", nullable = false)
    private Boolean cacheAllowed = Boolean.TRUE;

    @Builder.Default
    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion = CURRENT_POLICY_VERSION;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.cacheAllowed == null) {
            this.cacheAllowed = Boolean.TRUE;
        }
        if (this.policyVersion == null) {
            this.policyVersion = CURRENT_POLICY_VERSION;
        }
    }
}
