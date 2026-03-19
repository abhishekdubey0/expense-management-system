-- ============================================================
-- V1__create_users_table.sql
-- Auth Service - Initial schema
-- ============================================================

CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)     NOT NULL UNIQUE,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20),
    department  VARCHAR(100),
    employee_id VARCHAR(50)     UNIQUE,
    role        ENUM('EMPLOYEE', 'MANAGER', 'FINANCE_ADMIN', 'SUPER_ADMIN') NOT NULL DEFAULT 'EMPLOYEE',
    manager_id  BIGINT,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by  BIGINT,
    updated_by  BIGINT,

    PRIMARY KEY (id),
    CONSTRAINT fk_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for fast lookup
CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_uuid       ON users(uuid);
CREATE INDEX idx_users_role       ON users(role);
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_department ON users(department);

-- ============================================================
-- Refresh tokens table (for JWT rotation)
-- ============================================================

CREATE TABLE refresh_tokens (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    token       VARCHAR(512)    NOT NULL UNIQUE,
    user_id     BIGINT          NOT NULL,
    expires_at  DATETIME(6)     NOT NULL,
    is_revoked  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    device_info VARCHAR(255),
    ip_address  VARCHAR(50),

    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_token   ON refresh_tokens(token);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens(user_id);

-- ============================================================
-- Audit log table
-- ============================================================

CREATE TABLE audit_logs (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    action      VARCHAR(100)    NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(255),
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_action     ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- ============================================================
-- Default SUPER_ADMIN user (password: Admin@123)
-- BCrypt hash of 'Admin@123'
-- ============================================================

INSERT INTO users (uuid, email, password, first_name, last_name, role, is_active, is_verified)
VALUES (
    UUID(),
    'admin@expensemanager.com',
    '$2a$12$LcGpDWSy1Y8GCeQPpBniZOKkRYqBVfgjP2Z6fJ5uUxBj5wqQvMCPO',
    'Super',
    'Admin',
    'SUPER_ADMIN',
    TRUE,
    TRUE
);
