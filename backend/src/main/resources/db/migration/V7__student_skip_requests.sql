-- Flyway V7: Add student skip-request workflow columns on study_tasks

ALTER TABLE study_tasks
    ADD COLUMN skip_request_status VARCHAR(20) NULL,
    ADD COLUMN skip_request_reason VARCHAR(300) NULL,
    ADD COLUMN skip_requested_at TIMESTAMPTZ NULL,
    ADD COLUMN skip_reviewed_at TIMESTAMPTZ NULL,
    ADD COLUMN skip_reviewed_by_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE study_tasks
    ADD CONSTRAINT study_tasks_skip_request_status_check CHECK (
        skip_request_status IS NULL OR skip_request_status IN ('PENDING', 'APPROVED', 'REJECTED')
    );
