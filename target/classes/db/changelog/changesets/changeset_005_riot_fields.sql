--liquibase formatted sql

-- changeset thibault:ajout-metadata-match
ALTER TABLE matches
ADD COLUMN riot_match_id VARCHAR(50) UNIQUE,
ADD COLUMN game_version VARCHAR(20),
ADD COLUMN game_mode VARCHAR(50),
ADD COLUMN platform_id VARCHAR(10),
ADD COLUMN game_creation TIMESTAMP,
ADD COLUMN game_end TIMESTAMP,
ADD COLUMN game_duration BIGINT;

-- changeset thibault:ajout-participant-match-stats
ALTER TABLE participant_match_stats
DROP COLUMN gold,
DROP COLUMN vision,
DROP COLUMN cs,
DROP COLUMN damage,
ADD COLUMN champion_id INT,
ADD COLUMN champion_name VARCHAR(50),
ADD COLUMN gold_earned INT,
ADD COLUMN gold_per_minute INT,
ADD COLUMN total_damage_dealt INT,
ADD COLUMN physical_damage_dealt INT,
ADD COLUMN magic_damage_dealt INT,
ADD COLUMN damage_to_buildings INT,
ADD COLUMN total_minions_killed INT,
ADD COLUMN neutral_minions_killed INT,
ADD COLUMN dragon_kills INT,
ADD COLUMN total_heals INT,
ADD COLUMN vision_score INT,
ADD COLUMN first_blood_kill BOOLEAN,
ADD COLUMN first_tower_kill BOOLEAN,
ADD COLUMN largest_multi_kill INT,
ADD COLUMN lane VARCHAR(20),
ADD COLUMN win BOOLEAN,
ADD COLUMN participant_id INT,
ADD COLUMN puuid VARCHAR(100),
ADD COLUMN summoner_name VARCHAR(50);

-- changeset thibault:creation-table-team-match-stats
CREATE TABLE team_match_stats (
    id BIGSERIAL PRIMARY KEY,
    side VARCHAR(10),
    win BOOLEAN NOT NULL,
    baron_kills INT DEFAULT 0,
    dragon_kills INT DEFAULT 0,
    tower_kills INT DEFAULT 0,
    inhibitor_kills INT DEFAULT 0,
    rift_herald_kills INT DEFAULT 0,
    first_baron BOOLEAN DEFAULT FALSE,
    first_dragon BOOLEAN DEFAULT FALSE,
    first_inhibitor BOOLEAN DEFAULT FALSE,
    first_rift_herald BOOLEAN DEFAULT FALSE,
    first_tower BOOLEAN DEFAULT FALSE,
    first_blood BOOLEAN DEFAULT FALSE,
    id_match BIGINT NOT NULL,
    CONSTRAINT fk_team_match_stats_match FOREIGN KEY (id_match) REFERENCES matches(id) ON DELETE CASCADE
);

-- changeset thibault:ajout-index-team-match-stats
CREATE INDEX idx_team_match_stats_match ON team_match_stats(id_match);

-- changeset thibault:suppression-polymorphisme-participant
ALTER TABLE participants
ALTER COLUMN id_user SET NOT NULL,
DROP CONSTRAINT uq_participants_team_tournament;

ALTER TABLE matches
DROP COLUMN id_winner;

ALTER TABLE match_participants
ADD COLUMN is_winner BOOLEAN DEFAULT FALSE;