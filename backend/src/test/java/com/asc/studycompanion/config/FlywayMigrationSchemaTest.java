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

    @Test
    void v4MigrationAddsTechSubject() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V4__add_tech_subject.sql");
        assertTrue(resource.exists(), "V4 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("subjects_code_check"));
        assertTrue(sql.contains("'TECH'"));
        assertTrue(sql.contains("'Công nghệ'"));
    }

    @Test
    void v5MigrationLowersMinCharsTo400AndAddsSkippedTaskStatus() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V5__min_chars_400_and_task_skipped.sql");
        assertTrue(resource.exists(), "V5 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("BETWEEN 400 AND 3000"));
        assertTrue(sql.contains("'SKIPPED'"));
        assertTrue(sql.contains("study_tasks_status_check"));
    }

    @Test
    void v6MigrationExpandsAiEvaluationFeedbackTo1000Chars() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V6__expand_ai_feedback_1000.sql");
        assertTrue(resource.exists(), "V6 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("ai_evaluations"));
        assertTrue(sql.contains("VARCHAR(1000)"));
    }

    @Test
    void v7MigrationAddsStudentSkipRequestColumnsAndCheckConstraint() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V7__student_skip_requests.sql");
        assertTrue(resource.exists(), "V7 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("ALTER TABLE study_tasks"));
        assertTrue(sql.contains("skip_request_status VARCHAR(20) NULL"));
        assertTrue(sql.contains("skip_request_reason VARCHAR(300) NULL"));
        assertTrue(sql.contains("skip_requested_at TIMESTAMPTZ NULL"));
        assertTrue(sql.contains("skip_reviewed_at TIMESTAMPTZ NULL"));
        assertTrue(sql.contains("skip_reviewed_by_user_id BIGINT NULL"));
        assertTrue(sql.contains("study_tasks_skip_request_status_check"));
        assertTrue(sql.contains("'PENDING', 'APPROVED', 'REJECTED'"));
    }

    @Test
    void v8MigrationAddsGuardianDefaultDriveAndCustomTaskColumns() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V8__guardian_custom_tasks_and_drive.sql");
        assertTrue(resource.exists(), "V8 migration must exist on classpath");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("ALTER TABLE guardian_links"));
        assertTrue(sql.contains("default_drive_folder_url VARCHAR(500) NULL"));
        assertTrue(sql.contains("ALTER TABLE study_tasks"));
        assertTrue(sql.contains("assignment_mode VARCHAR(30) NOT NULL DEFAULT 'STANDARD_AI'"));
        assertTrue(sql.contains("custom_task_title VARCHAR(200) NULL"));
        assertTrue(sql.contains("custom_task_description VARCHAR(500) NULL"));
        assertTrue(sql.contains("custom_drive_folder_url VARCHAR(500) NULL"));
        assertTrue(sql.contains("custom_assigned_by_user_id BIGINT NULL"));
        assertTrue(sql.contains("custom_assigned_at TIMESTAMPTZ NULL"));
        assertTrue(sql.contains("custom_submission_note VARCHAR(300) NULL"));
        assertTrue(sql.contains("custom_submission_url VARCHAR(500) NULL"));
        assertTrue(sql.contains("custom_submitted_at TIMESTAMPTZ NULL"));
        assertTrue(sql.contains("study_tasks_assignment_mode_check"));
        assertTrue(sql.contains("'STANDARD_AI', 'CUSTOM_DRIVE'"));
    }
}
