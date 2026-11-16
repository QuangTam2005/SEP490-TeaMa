-- ============================================================================
-- SEP490 Capstone Copilot (SEP490-TeaMa)
-- Flyway Migration V2: Multi-Repo GitHub Support & Multi-PR WBS Tasks
-- Reference: docs/DESIGN.md (v1.3)
-- ============================================================================

CREATE TABLE project_repositories (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    repo_label      VARCHAR(50) NOT NULL,
    repo_full_name  VARCHAR(255) NOT NULL,
    github_repo_url VARCHAR(500) NOT NULL,
    webhook_secret  VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT REFERENCES users(id),
    updated_by      BIGINT REFERENCES users(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_project_repositories_active
    ON project_repositories (project_id, repo_full_name)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_project_repositories_project_id
    ON project_repositories (project_id)
    WHERE is_deleted = FALSE;

ALTER TABLE projects
    DROP COLUMN IF EXISTS github_repo_url,
    DROP COLUMN IF EXISTS webhook_secret;

ALTER TABLE wbs_tasks
    DROP COLUMN IF EXISTS github_pr_url,
    DROP COLUMN IF EXISTS github_pr_number,
    ADD COLUMN github_prs JSONB NOT NULL DEFAULT '[]'::jsonb;
