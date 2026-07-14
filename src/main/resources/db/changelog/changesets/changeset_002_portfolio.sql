--liquibase formatted sql

-- changeset claude:create-portfolios-table
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cash NUMERIC(14, 2) NOT NULL,
    starting_capital NUMERIC(14, 2) NOT NULL,
    total_taxes_paid NUMERIC(14, 2) NOT NULL DEFAULT 0,
    last_session_date DATE,
    streak INT NOT NULL DEFAULT 0,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_portfolios_user_id UNIQUE (user_id),
    CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- changeset claude:create-holdings-table
CREATE TABLE holdings (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    quantity NUMERIC(18, 6) NOT NULL,
    average_buy_price NUMERIC(14, 4) NOT NULL,
    CONSTRAINT uq_holdings_portfolio_ticker UNIQUE (portfolio_id, ticker),
    CONSTRAINT fk_holdings_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);

-- changeset claude:create-transactions-table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL,
    quantity NUMERIC(18, 6) NOT NULL,
    price NUMERIC(14, 4) NOT NULL,
    capital_gain NUMERIC(14, 2),
    tax NUMERIC(14, 2),
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id),
    CONSTRAINT chk_transactions_type CHECK (type IN ('BUY', 'SELL'))
);

-- changeset claude:create-portfolio-value-snapshots-table
CREATE TABLE portfolio_value_snapshots (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_value NUMERIC(14, 2) NOT NULL,
    CONSTRAINT uq_snapshots_portfolio_date UNIQUE (portfolio_id, snapshot_date),
    CONSTRAINT fk_snapshots_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);
