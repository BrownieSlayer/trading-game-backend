-- La base "trading" et l'utilisateur "trading" sont déjà créés par Postgres
-- à partir de POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD (docker-compose.yml).
-- Ce script ne crée que le schéma applicatif utilisé par Spring/Liquibase.
CREATE SCHEMA IF NOT EXISTS trading AUTHORIZATION trading;