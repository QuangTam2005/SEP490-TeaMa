-- ============================================================================
-- SEP490 Capstone Copilot (SEP490-TeaMa)
-- Flyway Migration V3: Test Case Execution Evidence & WBS Work Category
-- Reference: docs/DESIGN.md (v1.4)
-- ============================================================================

ALTER TABLE test_cases
    ADD COLUMN actual_result TEXT,
    ADD COLUMN executed_at TIMESTAMPTZ,
    ADD COLUMN evidence_attachments JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE wbs_tasks
    ADD COLUMN work_category VARCHAR(30) NOT NULL DEFAULT 'CODING',
    ADD CONSTRAINT chk_wbs_tasks_work_category CHECK (
        work_category IN ('REQUIREMENT', 'DESIGN', 'CODING', 'TESTING', 'PM')
    );

CREATE INDEX idx_wbs_tasks_work_category
    ON wbs_tasks (project_id, work_category)
    WHERE is_deleted = FALSE;
