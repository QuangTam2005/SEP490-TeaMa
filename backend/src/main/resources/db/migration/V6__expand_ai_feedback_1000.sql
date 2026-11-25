-- Flyway V6: Expand ai_evaluations.feedback length from 600 to 1000 chars for structured mentor feedback
ALTER TABLE ai_evaluations
    ALTER COLUMN feedback TYPE VARCHAR(1000);
