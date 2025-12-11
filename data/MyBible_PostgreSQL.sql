-- ============================================================================
-- MyBible Database Schema for PostgreSQL
-- Cloud SQL Instance: mybible-480818:us-central1:mybible-db
-- Database: mybible_db
-- ============================================================================

-- Connect to the database first:
-- \c mybible_db

-- ============================================================================
-- AUTH_USERS Table
-- Stores user authentication credentials
-- ============================================================================
DROP TABLE IF EXISTS AUTH_USERS CASCADE;

CREATE TABLE AUTH_USERS (
    ID VARCHAR(36) NOT NULL,
    EMAIL VARCHAR(255) NOT NULL,
    PASSWORD_HASH VARCHAR(255) NOT NULL,
    NAME VARCHAR(255),
    EMAIL_VERIFIED BOOLEAN NOT NULL DEFAULT FALSE,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LAST_LOGIN TIMESTAMP,

    CONSTRAINT AUTH_USERS_pk PRIMARY KEY (ID)
);

-- Index on email for login lookups
CREATE UNIQUE INDEX IDX_AUTH_USERS_EMAIL ON AUTH_USERS(LOWER(EMAIL));

-- ============================================================================
-- Grant permissions (run as superuser)
-- ============================================================================
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mybible_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mybible_user;
