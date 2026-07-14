--liquibase formatted sql

-- ======================================================================================
-- v0.0.0 - SCHEMA CORE
-- ======================================================================================

--changeset thibault:001
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_teams_name UNIQUE (name)
);

CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    id_sender BIGINT NOT NULL,
    id_receiver BIGINT NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_friends_pair UNIQUE (id_sender, id_receiver),
    CONSTRAINT fk_friends_sender FOREIGN KEY (id_sender) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_receiver FOREIGN KEY (id_receiver) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    id_team BIGINT NOT NULL,
    id_user BIGINT NOT NULL,
    CONSTRAINT uq_team_members UNIQUE (id_team, id_user),
    CONSTRAINT fk_team_members_team FOREIGN KEY (id_team) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    id_creator BIGINT,
    title VARCHAR(200) NOT NULL,
    format VARCHAR(50) NOT NULL,
    max_participants INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT uq_tournaments_title UNIQUE (title),
    CONSTRAINT fk_tournaments_creator FOREIGN KEY (id_creator) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE participants (
    id BIGSERIAL PRIMARY KEY,
    id_tournament BIGINT NOT NULL,
    id_user BIGINT,
    id_team BIGINT,
    total_points INT DEFAULT 0,
    CONSTRAINT fk_participants_tournament FOREIGN KEY (id_tournament) REFERENCES tournaments(id) ON DELETE CASCADE,
    CONSTRAINT fk_participants_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_participants_team FOREIGN KEY (id_team) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT uq_participants_user_tournament UNIQUE (id_user, id_tournament),
    CONSTRAINT uq_participants_team_tournament UNIQUE (id_team, id_tournament)
);

CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    id_tournament BIGINT NOT NULL,
    id_sender BIGINT NOT NULL,
    id_user BIGINT,
    id_team BIGINT,
    CONSTRAINT fk_requests_tournament FOREIGN KEY (id_tournament) REFERENCES tournaments(id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_sender FOREIGN KEY (id_sender) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_team FOREIGN KEY (id_team) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    id_tournament BIGINT NOT NULL,
    id_winner BIGINT,
    stage INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'SCHEDULED' NOT NULL,
    scheduled_date TIMESTAMP,
    match_format VARCHAR(10) DEFAULT 'BO1' NOT NULL, 
    CONSTRAINT fk_matches_tournament FOREIGN KEY (id_tournament) REFERENCES tournaments(id) ON DELETE CASCADE
);

CREATE TABLE match_participants (
    id BIGSERIAL PRIMARY KEY,
    id_match BIGINT NOT NULL,
    id_participant BIGINT NOT NULL,
    side VARCHAR(50) NOT NULL,
    points_earned INT DEFAULT 0,
    CONSTRAINT fk_match_participants_match FOREIGN KEY (id_match) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_participants_participant FOREIGN KEY (id_participant) REFERENCES participants(id) ON DELETE CASCADE,
    CONSTRAINT uq_match_participant_per_match UNIQUE (id_match, id_participant)
);

ALTER TABLE matches
ADD CONSTRAINT fk_matches_winner FOREIGN KEY (id_winner) REFERENCES match_participants(id) ON DELETE SET NULL;

CREATE TABLE participant_match_stats (
    id BIGSERIAL PRIMARY KEY,
    id_match_participant BIGINT NOT NULL UNIQUE,
    kills INT DEFAULT 0,
    deaths INT DEFAULT 0,
    assists INT DEFAULT 0,
    damage INT DEFAULT 0,
    cs INT DEFAULT 0,
    gold INT DEFAULT 0,

    CONSTRAINT fk_participant_match_stats_match_participant FOREIGN KEY (id_match_participant) REFERENCES match_participants(id) ON DELETE CASCADE
);

CREATE TABLE player_stats (
    id BIGSERIAL PRIMARY KEY,
    id_user BIGINT NOT NULL,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    kills INT DEFAULT 0,
    deaths INT DEFAULT 0,
    assists INT DEFAULT 0,
    updated_at TIMESTAMP,
    CONSTRAINT uq_player_stats_user UNIQUE (id_user),
    CONSTRAINT fk_player_stats_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE linked_account (
    id BIGSERIAL PRIMARY KEY,
    riot_id VARCHAR(100),
    puuid VARCHAR(100),
    rank_tier VARCHAR(50),
    rank_division VARCHAR(5),
    last_updated TIMESTAMP,
    id_user BIGINT NOT NULL,
    CONSTRAINT uq_user_id UNIQUE(id_user),
    CONSTRAINT fk_linked_accounts_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE
);
