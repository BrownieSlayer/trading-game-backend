--liquibase formatted sql

-- changeset thibault:add_username_and_auth_fields_to_user
ALTER TABLE users
DROP COLUMN email,
ADD COLUMN username VARCHAR(50) NOT NULL UNIQUE,
ADD COLUMN password VARCHAR(255) NOT NULL,
ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;

-- changeset thibault:replace_role_with_enum
ALTER TABLE users
ALTER COLUMN role TYPE VARCHAR(50) USING role::VARCHAR;

-- changeset thibault:add_vision_score_participant_match_stats
ALTER TABLE participant_match_stats
ADD COLUMN vision INT DEFAULT 0