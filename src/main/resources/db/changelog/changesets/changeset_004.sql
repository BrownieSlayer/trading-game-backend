--liquibase formatted sql

-- changeset damien:suppression_column_name
ALTER TABLE users
DROP COLUMN name