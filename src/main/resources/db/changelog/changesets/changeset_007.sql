--liquibase formatted sql

-- changeset thibault:ajout-des-champions
CREATE TABLE champions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);