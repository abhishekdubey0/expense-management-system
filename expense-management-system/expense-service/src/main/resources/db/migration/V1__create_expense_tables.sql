-- ============================================================
-- V1__create_expense_tables.sql
-- ============================================================

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon        VARCHAR(50),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id)
);

CREATE TABLE expenses (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    uuid            VARCHAR(36)     NOT NULL UNIQUE,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    amount          DECIMAL(15,2)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'INR',
    expense_date    DATE            NOT NULL,
    category_id     BIGINT          NOT NULL,
    status          ENUM('DRAFT','SUBMITTED','PENDING_APPROVAL','APPROVED','REJECTED','REIMBURSED')
                    NOT NULL DEFAULT 'DRAFT',
    submitter_id    VARCHAR(36)     NOT NULL COMMENT 'User UUID from auth-service',
    submitter_email VARCHAR(255)    NOT NULL,
    submitter_name  VARCHAR(200)    NOT NULL,
    department      VARCHAR(100),
    project_code    VARCHAR(50),
    receipt_url     VARCHAR(1000)   COMMENT 'S3 object key',
    receipt_name    VARCHAR(255),
    notes           TEXT,
    rejection_reason TEXT,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    version         BIGINT          NOT NULL DEFAULT 0 COMMENT 'For optimistic locking',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    submitted_at    DATETIME(6),
    approved_at     DATETIME(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Indexes for common query patterns
CREATE INDEX idx_expense_uuid         ON expenses(uuid);
CREATE INDEX idx_expense_submitter    ON expenses(submitter_id);
CREATE INDEX idx_expense_status       ON expenses(status);
CREATE INDEX idx_expense_date         ON expenses(expense_date);
CREATE INDEX idx_expense_category     ON expenses(category_id);
CREATE INDEX idx_expense_department   ON expenses(department);
CREATE INDEX idx_expense_deleted      ON expenses(is_deleted);
CREATE INDEX idx_expense_created_at   ON expenses(created_at);

-- Composite index for the most common query: user's non-deleted expenses
CREATE INDEX idx_expense_submitter_status ON expenses(submitter_id, status, is_deleted);

-- ============================================================
-- Expense audit log (separate from auth audit log)
-- ============================================================

CREATE TABLE expense_audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    expense_id  BIGINT       NOT NULL,
    action      VARCHAR(100) NOT NULL,
    old_status  VARCHAR(50),
    new_status  VARCHAR(50),
    performed_by VARCHAR(36) NOT NULL COMMENT 'User UUID',
    performed_by_name VARCHAR(200),
    comment     TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_audit_expense FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE INDEX idx_expense_audit_expense_id ON expense_audit_logs(expense_id);
CREATE INDEX idx_expense_audit_created_at ON expense_audit_logs(created_at);

-- ============================================================
-- Default categories seed data
-- ============================================================

INSERT INTO categories (name, description, icon) VALUES
('Travel',          'Flights, trains, local transport',   'airplane'),
('Accommodation',   'Hotels, guesthouses',                'hotel'),
('Meals',           'Food and beverages during work',     'food'),
('Office Supplies', 'Stationery, equipment',              'office'),
('Software',        'SaaS subscriptions, licenses',       'laptop'),
('Training',        'Courses, certifications, books',     'education'),
('Client Entertainment', 'Client meetings, events',       'entertainment'),
('Utilities',       'Internet, phone bills',              'utilities'),
('Medical',         'Work-related medical expenses',      'medical'),
('Miscellaneous',   'Other work-related expenses',        'misc');
