--liquibase formatted sql

-- changeset thibault:insert_test_users context:test
-- Mot de passe = "password" encodé en BCrypt
INSERT INTO users (username, password, role, enabled) 
VALUES 
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhka', 'ROLE_ADMIN', true),
    ('user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhka', 'ROLE_USER', true)
ON CONFLICT (username) DO NOTHING;
-- rollback DELETE FROM users WHERE username IN ('admin', 'user');