-- ============================================================
-- V1__create_approval_tables.sql
-- ============================================================

CREATE TABLE approval_requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    uuid            VARCHAR(36)     NOT NULL UNIQUE,
    expense_uuid    VARCHAR(36)     NOT NULL,
    expense_title   VARCHAR(255)    NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'INR',
    submitter_id    VARCHAR(36)     NOT NULL,
    submitter_email VARCHAR(255)    NOT NULL,
    submitter_name  VARCHAR(200)    NOT NULL,
    department      VARCHAR(100),
    current_level   INT             NOT NULL DEFAULT 1 COMMENT '1=Manager, 2=Finance',
    total_levels    INT             NOT NULL DEFAULT 1,
    status          ENUM('PENDING','APPROVED','REJECTED','ESCALATED') NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    completed_at    DATETIME(6),

    PRIMARY KEY (id)
);

CREATE INDEX idx_approval_expense_uuid  ON approval_requests(expense_uuid);
CREATE INDEX idx_approval_status        ON approval_requests(status);
CREATE INDEX idx_approval_submitter     ON approval_requests(submitter_id);
CREATE INDEX idx_approval_created_at    ON approval_requests(created_at);

-- ─────────────────────────────────────────────────────────────
-- Individual approval actions (one row per level per decision)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE approval_actions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    approval_request_id BIGINT          NOT NULL,
    level               INT             NOT NULL COMMENT '1=Manager, 2=Finance',
    action              ENUM('APPROVED','REJECTED','DELEGATED') NOT NULL,
    approver_id         VARCHAR(36)     NOT NULL,
    approver_email      VARCHAR(255)    NOT NULL,
    approver_name       VARCHAR(200)    NOT NULL,
    approver_role       VARCHAR(50)     NOT NULL,
    comment             TEXT,
    acted_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_action_request FOREIGN KEY (approval_request_id)
        REFERENCES approval_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_action_request_id ON approval_actions(approval_request_id);
CREATE INDEX idx_action_approver   ON approval_actions(approver_id);
CREATE INDEX idx_action_acted_at   ON approval_actions(acted_at);
