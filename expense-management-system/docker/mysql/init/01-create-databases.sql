-- ============================================================
-- docker/mysql/init/01-create-databases.sql
-- Runs automatically when MySQL container first starts.
-- Creates all databases for each microservice.
-- ============================================================

CREATE DATABASE IF NOT EXISTS auth_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS expense_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS approval_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS report_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant full access to app user on all service DBs
GRANT ALL PRIVILEGES ON auth_db.*     TO 'expenseuser'@'%';
GRANT ALL PRIVILEGES ON expense_db.*  TO 'expenseuser'@'%';
GRANT ALL PRIVILEGES ON approval_db.* TO 'expenseuser'@'%';
GRANT ALL PRIVILEGES ON report_db.*   TO 'expenseuser'@'%';

FLUSH PRIVILEGES;
