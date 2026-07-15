--liquibase formatted sql

-- changeset claude:create-business-states-table
CREATE TABLE business_states (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    investment NUMERIC(14, 2) NOT NULL DEFAULT 0,
    last_harvest_at TIMESTAMP,
    capacity_level INT NOT NULL DEFAULT 0,
    yield_level INT NOT NULL DEFAULT 0,
    storage_level INT NOT NULL DEFAULT 0,
    total_harvested NUMERIC(14, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_business_states_portfolio UNIQUE (portfolio_id),
    CONSTRAINT fk_business_states_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);

-- changeset claude:create-employment-states-table
CREATE TABLE employment_states (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    manual_level INT NOT NULL DEFAULT 0,
    intellect_level INT NOT NULL DEFAULT 0,
    computer_level INT NOT NULL DEFAULT 0,
    total_earned NUMERIC(14, 2) NOT NULL DEFAULT 0,
    active_job_title VARCHAR(100),
    active_job_tier VARCHAR(10),
    active_job_duration_hours INT,
    active_job_started_at TIMESTAMP,
    active_job_ends_at TIMESTAMP,
    active_job_hourly_wage NUMERIC(14, 2),
    active_job_total_wage NUMERIC(14, 2),
    CONSTRAINT uq_employment_states_portfolio UNIQUE (portfolio_id),
    CONSTRAINT fk_employment_states_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);

-- changeset claude:create-job-offers-table
CREATE TABLE job_offers (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    tier VARCHAR(10) NOT NULL,
    duration_hours INT NOT NULL,
    hourly_wage NUMERIC(14, 2) NOT NULL,
    total_wage NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_job_offers_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);
