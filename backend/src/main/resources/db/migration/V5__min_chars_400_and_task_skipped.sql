-- Flyway V5: Lower minimum submission char_count to 400 and allow SKIPPED status on study_tasks

-- 1. Update study_submissions.char_count check constraint to BETWEEN 400 AND 3000
DO $$
DECLARE
    c_name text;
BEGIN
    FOR c_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'study_submissions'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%char_count%'
    LOOP
        EXECUTE format('ALTER TABLE study_submissions DROP CONSTRAINT %I', c_name);
    END LOOP;
END $$;

ALTER TABLE study_submissions
    ADD CONSTRAINT study_submissions_char_count_check CHECK (char_count BETWEEN 400 AND 3000);

-- 2. Allow SKIPPED status on study_tasks (Guardian can mark review/preview tasks as skipped)
DO $$
DECLARE
    c_name text;
BEGIN
    FOR c_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'study_tasks'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE study_tasks DROP CONSTRAINT %I', c_name);
    END LOOP;
END $$;

ALTER TABLE study_tasks
    ADD CONSTRAINT study_tasks_status_check CHECK (
        status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'EVALUATING', 'EVALUATED', 'EVALUATION_FAILED', 'BLOCKED', 'SKIPPED')
    );
