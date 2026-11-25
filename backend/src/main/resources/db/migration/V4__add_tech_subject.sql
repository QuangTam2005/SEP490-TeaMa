-- Flyway V4: Add Technology (Công nghệ) subject for Grades 3-12

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
        code IN ('MATH', 'LIT', 'ENG', 'SCI', 'HISGEO', 'PHY', 'CHEM', 'BIO', 'HIST', 'GEO', 'INFO', 'TECH', 'CIVICS')
    );

INSERT INTO subjects (code, name, is_active, min_grade, max_grade) VALUES
    ('TECH', 'Công nghệ', TRUE, 3, 12)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_active = EXCLUDED.is_active,
    min_grade = EXCLUDED.min_grade,
    max_grade = EXCLUDED.max_grade;
