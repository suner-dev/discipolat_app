-- V5__fix_password_hashes.sql
-- Fix: Replace PLACEHOLDER passwords with real BCrypt hashes
-- This migration ensures existing databases get proper password hashes
-- without requiring a database volume reset.
--
-- The hash below is the BCrypt (cost 10) of "password123"
-- Pre-computed: $2a$10$xf6qwOh4g8AidlGwgyD8S.Vbl7FVv3dNkO5GI7.iE/dgrveA5/j..

-- 1. Fix PLACEHOLDER passwords with real BCrypt hash
UPDATE users
SET password_hash = '$2a$10$xf6qwOh4g8AidlGwgyD8S.Vbl7FVv3dNkO5GI7.iE/dgrveA5/j..',
    updated_at = CURRENT_TIMESTAMP
WHERE password_hash = 'PLACEHOLDER';

-- 2. Reset any locked accounts (PLACEHOLDER passwords already handled above)
UPDATE users
SET failed_login_attempts = 0,
    account_locked_until = NULL
WHERE failed_login_attempts > 0;
