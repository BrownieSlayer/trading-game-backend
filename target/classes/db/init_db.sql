-- Créer la base
CREATE DATABASE ignis;

-- Créer l'utilisateur
CREATE USER ignis WITH PASSWORD 'ignis';

-- Créer le schéma ignis
CREATE SCHEMA ignis AUTHORIZATION ignis;

-- Donner tous les droits sur la base à l'utilisateur
GRANT ALL PRIVILEGES ON DATABASE ignis TO ignis;