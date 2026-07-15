--liquibase formatted sql

-- changeset claude:create-bet-pari-table
CREATE TABLE bet_pari (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    stake NUMERIC(14, 2) NOT NULL,
    reference_price NUMERIC(14, 4) NOT NULL,
    placed_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    result_price NUMERIC(14, 4),
    gain NUMERIC(14, 2),
    resolved_date DATE,
    CONSTRAINT fk_bet_pari_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id),
    CONSTRAINT chk_bet_pari_direction CHECK (direction IN ('UP', 'DOWN')),
    CONSTRAINT chk_bet_pari_status CHECK (status IN ('ACTIVE', 'WON', 'LOST'))
);

-- Un seul pari actif à la fois par portefeuille.
CREATE UNIQUE INDEX uq_bet_pari_one_active_per_portfolio ON bet_pari (portfolio_id) WHERE status = 'ACTIVE';

-- changeset claude:create-wheel-spins-table
CREATE TABLE wheel_spins (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    spin_date DATE NOT NULL,
    label VARCHAR(100) NOT NULL,
    gain NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_wheel_spins_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id),
    CONSTRAINT uq_wheel_spins_portfolio_date UNIQUE (portfolio_id, spin_date)
);
