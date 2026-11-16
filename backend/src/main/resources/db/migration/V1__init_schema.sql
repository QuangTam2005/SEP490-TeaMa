-- ============================================================================
-- SEP490 Capstone Copilot (SEP490-TeaMa)
-- Flyway Migration V1: Initial PostgreSQL 16 Schema (20 tables / 17 ERD groups)
-- Reference: docs/DESIGN.md (v1.2 - Part II)
-- ============================================================================

-- ============================================================================
-- 1. Identity & Project Core
-- ============================================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    student_code    VARCHAR(20),
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
    id                      BIGSERIAL PRIMARY KEY,
    project_code            VARCHAR(30) NOT NULL,
    project_name            VARCHAR(255) NOT NULL,
    group_name              VARCHAR(50) NOT NULL,
    software_type           VARCHAR(100),
    start_date              DATE NOT NULL,
    github_repo_url         VARCHAR(500),
    webhook_secret          VARCHAR(255),
    supervisor_magic_token  UUID NOT NULL UNIQUE,
    supervisor_name         VARCHAR(150),
    supervisor_email        VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT REFERENCES users(id),
    updated_by              BIGINT REFERENCES users(id),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE project_members (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    role        VARCHAR(20) NOT NULL,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_members_role CHECK (role IN ('LEADER', 'MEMBER'))
);

CREATE INDEX idx_project_members_user_id ON project_members(user_id);

-- ============================================================================
-- 2. Milestones & Deliverables (SEP490 15-Week Track)
-- ============================================================================

CREATE TABLE milestones (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    milestone_code      VARCHAR(10) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    week_number         INT NOT NULL,
    due_date            DATE,
    oga_weight_percent  NUMERIC(5, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_milestones_project_code UNIQUE (project_id, milestone_code),
    CONSTRAINT chk_milestones_week CHECK (week_number BETWEEN 1 AND 15),
    CONSTRAINT chk_milestones_status CHECK (status IN ('PENDING', 'SUBMITTED', 'APPROVED'))
);

CREATE TABLE milestone_deliverables (
    id              BIGSERIAL PRIMARY KEY,
    milestone_id    BIGINT NOT NULL REFERENCES milestones(id),
    item_name       VARCHAR(255) NOT NULL,
    is_required     BOOLEAN NOT NULL DEFAULT TRUE,
    file_path       VARCHAR(500),
    file_name       VARCHAR(255),
    file_size       BIGINT,
    uploaded_by     BIGINT REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_milestone_deliverables_status CHECK (status IN ('PENDING', 'SUBMITTED', 'APPROVED'))
);

CREATE INDEX idx_milestone_deliverables_milestone_id ON milestone_deliverables(milestone_id);

-- ============================================================================
-- 3. Planning & Report 2 Entities
-- ============================================================================

CREATE TABLE features (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT REFERENCES users(id),
    updated_by  BIGINT REFERENCES users(id),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_features_project_code
    ON features (project_id, code)
    WHERE is_deleted = FALSE;

CREATE TABLE raci_assignments (
    id                      BIGSERIAL PRIMARY KEY,
    project_id              BIGINT NOT NULL REFERENCES projects(id),
    deliverable_or_task_name VARCHAR(255) NOT NULL,
    member_id               BIGINT NOT NULL REFERENCES project_members(id),
    raci_role               VARCHAR(5),
    CONSTRAINT uq_raci_project_task_member UNIQUE (project_id, deliverable_or_task_name, member_id),
    CONSTRAINT chk_raci_role CHECK (raci_role IS NULL OR raci_role IN ('D', 'R', 'S', 'I'))
);

CREATE TABLE project_risks (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    risk_description    TEXT NOT NULL,
    impact              VARCHAR(20) NOT NULL,
    possibility         VARCHAR(20) NOT NULL,
    response_plan       TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          BIGINT REFERENCES users(id),
    updated_by          BIGINT REFERENCES users(id),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_project_risks_impact CHECK (impact IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_project_risks_possibility CHECK (possibility IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX idx_project_risks_project_id ON project_risks(project_id) WHERE is_deleted = FALSE;

CREATE TABLE quality_objectives (
    id                          BIGSERIAL PRIMARY KEY,
    project_id                  BIGINT NOT NULL REFERENCES projects(id),
    testing_stage               VARCHAR(30) NOT NULL,
    target_coverage_percent     NUMERIC(5, 2) NOT NULL DEFAULT 80.00,
    target_defect_rate_percent  NUMERIC(5, 2) NOT NULL DEFAULT 5.00,
    CONSTRAINT uq_quality_objectives_project_stage UNIQUE (project_id, testing_stage),
    CONSTRAINT chk_quality_objectives_stage CHECK (
        testing_stage IN ('REVIEWING', 'UNIT_TEST', 'INTEGRATION_TEST', 'SYSTEM_TEST', 'ACCEPTANCE_TEST')
    )
);

-- ============================================================================
-- 4. SRS & Report 3 Entities
-- ============================================================================

CREATE TABLE system_actors (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT REFERENCES users(id),
    updated_by  BIGINT REFERENCES users(id),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_system_actors_project_name
    ON system_actors (project_id, name)
    WHERE is_deleted = FALSE;

CREATE TABLE use_cases (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    feature_id  BIGINT NOT NULL REFERENCES features(id),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    actors      VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT REFERENCES users(id),
    updated_by  BIGINT REFERENCES users(id),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_use_cases_project_code
    ON use_cases (project_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_use_cases_feature_id ON use_cases(feature_id) WHERE is_deleted = FALSE;

CREATE TABLE screens_functions (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    use_case_id         BIGINT REFERENCES use_cases(id),
    code                VARCHAR(20) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(20) NOT NULL,
    description         TEXT,
    field_count         INT NOT NULL DEFAULT 0,
    transaction_count   INT NOT NULL DEFAULT 0,
    auth_matrix_json    JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          BIGINT REFERENCES users(id),
    updated_by          BIGINT REFERENCES users(id),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_screens_functions_type CHECK (type IN ('SCREEN', 'NON_SCREEN'))
);

CREATE UNIQUE INDEX uq_screens_functions_project_code
    ON screens_functions (project_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_screens_functions_use_case_id ON screens_functions(use_case_id) WHERE is_deleted = FALSE;

CREATE TABLE wbs_tasks (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    feature_id          BIGINT REFERENCES features(id),
    use_case_id         BIGINT REFERENCES use_cases(id),
    screen_function_id  BIGINT REFERENCES screens_functions(id),
    task_code           VARCHAR(20) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    field_count         INT NOT NULL DEFAULT 0,
    transaction_count   INT NOT NULL DEFAULT 0,
    complexity          VARCHAR(20) NOT NULL DEFAULT 'SIMPLE',
    est_mandays         NUMERIC(5, 2) NOT NULL DEFAULT 1.0,
    actual_mandays      NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    iteration           VARCHAR(20) NOT NULL DEFAULT 'ITER_1',
    status              VARCHAR(20) NOT NULL DEFAULT 'TODO',
    assignee_id         BIGINT REFERENCES users(id),
    github_pr_url       VARCHAR(500),
    github_pr_number    INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          BIGINT REFERENCES users(id),
    updated_by          BIGINT REFERENCES users(id),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_wbs_tasks_complexity CHECK (complexity IN ('SIMPLE', 'MEDIUM', 'COMPLEX')),
    CONSTRAINT chk_wbs_tasks_iteration CHECK (iteration IN ('ITER_1', 'ITER_2', 'ITER_3', 'ITER_4')),
    CONSTRAINT chk_wbs_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'))
);

CREATE UNIQUE INDEX uq_wbs_tasks_project_task_code
    ON wbs_tasks (project_id, task_code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_wbs_tasks_project_iter ON wbs_tasks(project_id, iteration) WHERE is_deleted = FALSE;
CREATE INDEX idx_wbs_tasks_assignee_id ON wbs_tasks(assignee_id) WHERE is_deleted = FALSE;

CREATE TABLE business_rules (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    code        VARCHAR(20) NOT NULL,
    definition  TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT REFERENCES users(id),
    updated_by  BIGINT REFERENCES users(id),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_business_rules_project_code
    ON business_rules (project_id, code)
    WHERE is_deleted = FALSE;

CREATE TABLE app_messages (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    code            VARCHAR(20) NOT NULL,
    message_type    VARCHAR(20) NOT NULL,
    context         TEXT NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT REFERENCES users(id),
    updated_by      BIGINT REFERENCES users(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_app_messages_type CHECK (message_type IN ('INLINE', 'TOAST', 'FIELD_ERROR'))
);

CREATE UNIQUE INDEX uq_app_messages_project_code
    ON app_messages (project_id, code)
    WHERE is_deleted = FALSE;

CREATE TABLE diagram_attachments (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    diagram_type    VARCHAR(30) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT REFERENCES users(id),
    updated_by      BIGINT REFERENCES users(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_diagram_attachments_type CHECK (
        diagram_type IN ('CONTEXT', 'USE_CASE', 'SCREEN_FLOW', 'ERD', 'ARCHITECTURE')
    )
);

CREATE INDEX idx_diagram_attachments_project_id ON diagram_attachments(project_id) WHERE is_deleted = FALSE;

-- ============================================================================
-- 5. Testing, Operational Tracking, Peer-Review & Audit Logs
-- ============================================================================

CREATE TABLE test_cases (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    use_case_id     BIGINT NOT NULL REFERENCES use_cases(id),
    code            VARCHAR(20) NOT NULL,
    test_level      VARCHAR(10) NOT NULL,
    description     TEXT NOT NULL,
    expected_result TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNTESTED',
    executed_by     BIGINT REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT REFERENCES users(id),
    updated_by      BIGINT REFERENCES users(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_test_cases_level CHECK (test_level IN ('UT', 'IT', 'ST', 'AT')),
    CONSTRAINT chk_test_cases_status CHECK (status IN ('UNTESTED', 'PASS', 'FAIL'))
);

CREATE UNIQUE INDEX uq_test_cases_project_code
    ON test_cases (project_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_test_cases_use_case_id ON test_cases(use_case_id) WHERE is_deleted = FALSE;

CREATE TABLE tracking_logs (
    id                      BIGSERIAL PRIMARY KEY,
    project_id              BIGINT NOT NULL REFERENCES projects(id),
    log_type                VARCHAR(20) NOT NULL,
    code                    VARCHAR(20) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    description             TEXT,
    priority                VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    potential_impact        TEXT,
    screen_function_id      BIGINT REFERENCES screens_functions(id),
    related_test_case_id    BIGINT REFERENCES test_cases(id),
    raised_by               VARCHAR(150),
    assignee_id             BIGINT REFERENCES users(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at               TIMESTAMPTZ,
    answer_or_resolution    TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT REFERENCES users(id),
    updated_by              BIGINT REFERENCES users(id),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_tracking_logs_type CHECK (log_type IN ('ISSUE', 'DEFECT', 'QA', 'WEEKLY_MATTER')),
    CONSTRAINT chk_tracking_logs_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_tracking_logs_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'PENDING'))
);

CREATE UNIQUE INDEX uq_tracking_logs_project_code
    ON tracking_logs (project_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_tracking_logs_project_type ON tracking_logs(project_id, log_type) WHERE is_deleted = FALSE;

CREATE TABLE peer_reviews (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    iteration           VARCHAR(20) NOT NULL,
    reviewer_id         BIGINT NOT NULL REFERENCES users(id),
    reviewee_id         BIGINT NOT NULL REFERENCES users(id),
    attitude_score      INT NOT NULL CHECK (attitude_score BETWEEN 1 AND 10),
    timeliness_score    INT NOT NULL CHECK (timeliness_score BETWEEN 1 AND 10),
    quality_score       INT NOT NULL CHECK (quality_score BETWEEN 1 AND 10),
    comment             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_peer_reviews_project_iter_pair UNIQUE (project_id, iteration, reviewer_id, reviewee_id),
    CONSTRAINT chk_peer_reviews_iteration CHECK (iteration IN ('ITER_1', 'ITER_2', 'ITER_3', 'ITER_4')),
    CONSTRAINT chk_peer_reviews_not_self CHECK (reviewer_id <> reviewee_id)
);

CREATE TABLE document_change_logs (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id),
    report_target       VARCHAR(10) NOT NULL,
    change_type         VARCHAR(5) NOT NULL,
    in_charge_user_id   BIGINT REFERENCES users(id),
    change_description  TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_document_change_logs_report CHECK (report_target IN ('R1', 'R2', 'R3', 'R4', 'R5', 'R6', 'R7')),
    CONSTRAINT chk_document_change_logs_type CHECK (change_type IN ('A', 'M', 'D'))
);

CREATE INDEX idx_document_change_logs_project_created ON document_change_logs(project_id, created_at DESC);
