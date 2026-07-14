--liquibase formatted sql

-- changeset thibault:ajout-typage-requete
ALTER TABLE requests ADD COLUMN type VARCHAR(20) NOT NULL;
ALTER TABLE requests RENAME COLUMN id_user TO id_receiver;
ALTER TABLE requests ALTER COLUMN id_tournament DROP NOT NULL;