-- Flyway V8: Support Guardian custom task assignments replacing standard AI review/preview with Google Drive folder submission

ALTER TABLE guardian_links
    ADD COLUMN default_drive_folder_url VARCHAR(500) NULL;

ALTER TABLE study_tasks
    ADD COLUMN assignment_mode VARCHAR(30) NOT NULL DEFAULT 'STANDARD_AI',
    ADD COLUMN custom_task_title VARCHAR(200) NULL,
    ADD COLUMN custom_task_description VARCHAR(500) NULL,
    ADD COLUMN custom_drive_folder_url VARCHAR(500) NULL,
    ADD COLUMN custom_assigned_by_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN custom_assigned_at TIMESTAMPTZ NULL,
    ADD COLUMN custom_submission_note VARCHAR(300) NULL,
    ADD COLUMN custom_submission_url VARCHAR(500) NULL,
    ADD COLUMN custom_submitted_at TIMESTAMPTZ NULL;

ALTER TABLE study_tasks
    ADD CONSTRAINT study_tasks_assignment_mode_check CHECK (
        assignment_mode IN ('STANDARD_AI', 'CUSTOM_DRIVE')
    );
