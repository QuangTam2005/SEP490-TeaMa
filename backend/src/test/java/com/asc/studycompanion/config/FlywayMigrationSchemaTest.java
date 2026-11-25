package com.asc.studycompanion.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationSchemaTest {

    @Test
    void v1MigrationContainsAuthCurriculumTablesAndSeedsFiveGrade6Subjects() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__init_auth_and_curriculum_schema.sql");
        assertTrue(resource.exists(), "V1 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE users"));
        assertTrue(sql.contains("CREATE TABLE student_profiles"));
        assertTrue(sql.contains("CREATE TABLE guardian_links"));
        assertTrue(sql.contains("CREATE TABLE subjects"));
        assertTrue(sql.contains("CREATE TABLE timetables"));
        assertTrue(sql.contains("CREATE TABLE timetable_entries"));

        assertTrue(sql.contains("'MATH'"));
        assertTrue(sql.contains("'LIT'"));
        assertTrue(sql.contains("'ENG'"));
        assertTrue(sql.contains("'SCI'"));
        assertTrue(sql.contains("'HISGEO'"));
    }

    @Test
    void v2MigrationContainsAllStudyAndAiTablesWithDomainConstraints() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__init_study_and_ai_schema.sql");
        assertTrue(resource.exists(), "V2 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE study_plans"));
        assertTrue(sql.contains("CREATE TABLE study_tasks"));
        assertTrue(sql.contains("CREATE TABLE study_submissions"));
        assertTrue(sql.contains("CREATE TABLE ai_evaluations"));
        assertTrue(sql.contains("CREATE TABLE source_references"));
        assertTrue(sql.contains("CREATE TABLE evaluation_jobs"));
        assertTrue(sql.contains("CREATE TABLE ai_usage_logs"));
        assertTrue(sql.contains("CREATE TABLE knowledge_cache"));

        assertTrue(sql.contains("BETWEEN 850 AND 2000"));
        assertTrue(sql.contains("BETWEEN 1 AND 3"));
        assertTrue(sql.contains("BETWEEN 1 AND 12"));
        assertTrue(sql.contains("feedback VARCHAR(600)"));
    }

    @Test
    void v3MigrationExpandsCharCountRangeSubjectsAndTaskGuardianColumns() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V3__expand_features_and_subjects.sql");
        assertTrue(resource.exists(), "V3 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("BETWEEN 850 AND 3000"));
        assertTrue(sql.contains("min_grade"));
        assertTrue(sql.contains("max_grade"));
        assertTrue(sql.contains("'PHY'"));
        assertTrue(sql.contains("'CHEM'"));
        assertTrue(sql.contains("'BIO'"));
        assertTrue(sql.contains("'HIST'"));
        assertTrue(sql.contains("'GEO'"));
        assertTrue(sql.contains("'INFO'"));
        assertTrue(sql.contains("'CIVICS'"));
        assertTrue(sql.contains("guardian_acknowledged_at"));
        assertTrue(sql.contains("guardian_note"));
        assertTrue(sql.contains("guardian_acknowledged_by_user_id"));
    }
}
