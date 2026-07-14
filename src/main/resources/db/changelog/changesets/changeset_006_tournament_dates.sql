--liquibase formatted sql

-- changeset thibault:ajout-dates-tournoi
ALTER TABLE tournaments 
ADD COLUMN start_date TIMESTAMP,
ADD COLUMN end_date TIMESTAMP;

UPDATE tournaments 
SET start_date = CURRENT_TIMESTAMP,
    end_date = CURRENT_TIMESTAMP + INTERVAL '7 days'
WHERE start_date IS NULL;

ALTER TABLE tournaments 
ADD CONSTRAINT chk_tournaments_dates 
CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);