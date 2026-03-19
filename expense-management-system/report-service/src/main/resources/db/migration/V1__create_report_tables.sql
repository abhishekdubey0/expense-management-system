-- ============================================================
-- V1__create_report_tables.sql
-- Report Service – stores denormalized expense snapshots
-- for high-performance reporting without hitting expense-service
-- ============================================================

CREATE TABLE expense_snapshots (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    expense_uuid    VARCHAR(36)     NOT NULL UNIQUE,
    title           VARCHAR(255)    NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'INR',
    expense_date    DATE            NOT NULL,
    category_name   VARCHAR(100),
    status          VARCHAR(30)     NOT NULL,
    submitter_id    VARCHAR(36)     NOT NULL,
    submitter_email VARCHAR(255)    NOT NULL,
    submitter_name  VARCHAR(200)    NOT NULL,
    department      VARCHAR(100),
    project_code    VARCHAR(50),
    created_at      DATETIME(6)     NOT NULL,
    approved_at     DATETIME(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id)
);

CREATE INDEX idx_snap_uuid          ON expense_snapshots(expense_uuid);
CREATE INDEX idx_snap_submitter     ON expense_snapshots(submitter_id);
CREATE INDEX idx_snap_status        ON expense_snapshots(status);
CREATE INDEX idx_snap_date          ON expense_snapshots(expense_date);
CREATE INDEX idx_snap_department    ON expense_snapshots(department);
CREATE INDEX idx_snap_category      ON expense_snapshots(category_name);
CREATE INDEX idx_snap_submitter_date ON expense_snapshots(submitter_id, expense_date);
CREATE INDEX idx_snap_dept_date     ON expense_snapshots(department, expense_date);
