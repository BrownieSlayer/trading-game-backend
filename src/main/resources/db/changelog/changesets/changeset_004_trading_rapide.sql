--liquibase formatted sql

-- changeset claude:create-reflex-trading-sessions-table
CREATE TABLE reflex_trading_sessions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    started_at TIMESTAMP NOT NULL,
    prices VARCHAR(1000) NOT NULL,
    buy_tick_index INT,
    sell_tick_index INT,
    gain NUMERIC(14, 2),
    abandoned BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_reflex_sessions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id),
    CONSTRAINT chk_reflex_sessions_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

-- Une seule partie en cours à la fois par portefeuille.
CREATE UNIQUE INDEX uq_reflex_sessions_one_open_per_portfolio ON reflex_trading_sessions (portfolio_id) WHERE status = 'OPEN';
