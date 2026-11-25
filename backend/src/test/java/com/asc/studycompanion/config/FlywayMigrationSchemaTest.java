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
}
