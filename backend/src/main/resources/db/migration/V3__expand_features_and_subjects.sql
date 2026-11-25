-- Flyway V3: Expand submission char limit (850..3000), multi-grade subjects (1..12), and parent acknowledgment/note on study_tasks

-- 1. Upgrade study_submissions.char_count check constraint to BETWEEN 850 AND 3000
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
    ADD CONSTRAINT study_submissions_char_count_check CHECK (char_count BETWEEN 850 AND 3000);

-- 2. Expand subjects with min_grade / max_grade and grades 1-12 curriculum subjects
ALTER TABLE subjects
    ADD COLUMN IF NOT EXISTS min_grade INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS max_grade INT NOT NULL DEFAULT 12;

DO $$
DECLARE
    c_name text;
BEGIN
    FOR c_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'subjects'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%code%'
    LOOP
        EXECUTE format('ALTER TABLE subjects DROP CONSTRAINT %I', c_name);
    END LOOP;
END $$;

ALTER TABLE subjects
    ADD CONSTRAINT subjects_code_check CHECK (
        code IN ('MATH', 'LIT', 'ENG', 'SCI', 'HISGEO', 'PHY', 'CHEM', 'BIO', 'HIST', 'GEO', 'INFO', 'CIVICS')
    ),
    ADD CONSTRAINT subjects_grade_range_check CHECK (
        min_grade BETWEEN 1 AND 12 AND max_grade BETWEEN 1 AND 12 AND min_grade <= max_grade
    );

UPDATE subjects SET min_grade = 1, max_grade = 12 WHERE code IN ('MATH', 'LIT', 'ENG');
UPDATE subjects SET min_grade = 1, max_grade = 9 WHERE code IN ('SCI', 'HISGEO');

INSERT INTO subjects (code, name, is_active, min_grade, max_grade) VALUES
    ('PHY', 'Vật lý', TRUE, 10, 12),
    ('CHEM', 'Hóa học', TRUE, 10, 12),
    ('BIO', 'Sinh học', TRUE, 10, 12),
    ('HIST', 'Lịch sử', TRUE, 10, 12),
    ('GEO', 'Địa lý', TRUE, 10, 12),
    ('INFO', 'Tin học', TRUE, 3, 12),
    ('CIVICS', 'Giáo dục công dân', TRUE, 6, 12)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_active = EXCLUDED.is_active,
    min_grade = EXCLUDED.min_grade,
    max_grade = EXCLUDED.max_grade;

-- 3. Add guardian acknowledgment and encouragement note columns to study_tasks
ALTER TABLE study_tasks
    ADD COLUMN IF NOT EXISTS guardian_acknowledged_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS guardian_note VARCHAR(300),
    ADD COLUMN IF NOT EXISTS guardian_acknowledged_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
