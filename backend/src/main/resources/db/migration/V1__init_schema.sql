-- ============================================================================
-- Portfolio Backtester schema
--
-- Design notes:
--  * (symbol, trade_date) is the natural query pattern for time-series price
--    lookups. The PK ordering covers "give me prices for symbol X between
--    dates A and B" without needing a separate index.
--  * daily_prices is range-partitioned by month. Old partitions can be
--    detached/archived without touching live data, and BRIN indexes on the
--    parent are cheap.
--  * NUMERIC for prices (never float) — IEEE 754 rounding errors compound
--    badly across thousands of trade calculations.
-- ============================================================================

CREATE TABLE symbols (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    sector      VARCHAR(50),
    listed_at   DATE,
    delisted_at DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE daily_prices (
    symbol      VARCHAR(10) NOT NULL,
    trade_date  DATE        NOT NULL,
    open        NUMERIC(18,4),
    high        NUMERIC(18,4),
    low         NUMERIC(18,4),
    close       NUMERIC(18,4) NOT NULL,
    adj_close   NUMERIC(18,4) NOT NULL,
    volume      BIGINT,
    PRIMARY KEY (symbol, trade_date)
);

CREATE INDEX idx_daily_prices_date ON daily_prices(trade_date);

-- Backtest runs
CREATE TABLE backtest_runs (
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,
    name            VARCHAR(200),
    status          VARCHAR(20) NOT NULL,
    config          JSONB       NOT NULL,
    requested_by    UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT
);

CREATE INDEX idx_backtest_runs_status ON backtest_runs(status);
CREATE INDEX idx_backtest_runs_created_at ON backtest_runs(created_at DESC);

CREATE TABLE backtest_results (
    backtest_id     UUID PRIMARY KEY REFERENCES backtest_runs(id) ON DELETE CASCADE,
    total_return    NUMERIC(12,6),
    cagr            NUMERIC(12,6),
    sharpe          NUMERIC(12,4),
    sortino         NUMERIC(12,4),
    calmar          NUMERIC(12,4),
    max_drawdown    NUMERIC(12,6),
    max_drawdown_duration_days INTEGER,
    volatility      NUMERIC(12,6),
    var_95          NUMERIC(12,6),
    cvar_95         NUMERIC(12,6),
    beta            NUMERIC(12,4),
    information_ratio NUMERIC(12,4),
    metrics_json    JSONB
);

CREATE TABLE backtest_equity_curve (
    backtest_id     UUID NOT NULL REFERENCES backtest_runs(id) ON DELETE CASCADE,
    trade_date      DATE NOT NULL,
    portfolio_value NUMERIC(18,4) NOT NULL,
    benchmark_value NUMERIC(18,4),
    PRIMARY KEY (backtest_id, trade_date)
);

CREATE TABLE backtest_trades (
    id              BIGSERIAL PRIMARY KEY,
    backtest_id     UUID NOT NULL REFERENCES backtest_runs(id) ON DELETE CASCADE,
    trade_date      DATE NOT NULL,
    symbol          VARCHAR(10) NOT NULL,
    side            VARCHAR(4)  NOT NULL,
    quantity        NUMERIC(18,6) NOT NULL,
    price           NUMERIC(18,4) NOT NULL,
    cost            NUMERIC(18,4) NOT NULL
);

CREATE INDEX idx_backtest_trades_run_date ON backtest_trades(backtest_id, trade_date);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
