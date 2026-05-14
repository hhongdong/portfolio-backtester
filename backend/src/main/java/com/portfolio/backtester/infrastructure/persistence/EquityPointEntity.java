package com.portfolio.backtester.infrastructure.persistence;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "backtest_equity_curve")
public class EquityPointEntity {

    @EmbeddedId
    private Key id;

    @Column(name = "portfolio_value", nullable = false)
    private BigDecimal portfolioValue;

    @Column(name = "benchmark_value")
    private BigDecimal benchmarkValue;

    public EquityPointEntity() {}

    public EquityPointEntity(UUID backtestId, LocalDate date,
                             BigDecimal portfolioValue, BigDecimal benchmarkValue) {
        this.id = new Key(backtestId, date);
        this.portfolioValue = portfolioValue;
        this.benchmarkValue = benchmarkValue;
    }

    public UUID getBacktestId() { return id.backtestId; }
    public LocalDate getTradeDate() { return id.tradeDate; }
    public BigDecimal getPortfolioValue() { return portfolioValue; }
    public BigDecimal getBenchmarkValue() { return benchmarkValue; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "backtest_id", nullable = false) private UUID backtestId;
        @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;

        public Key() {}
        public Key(UUID backtestId, LocalDate tradeDate) {
            this.backtestId = backtestId;
            this.tradeDate = tradeDate;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(backtestId, k.backtestId)
                    && Objects.equals(tradeDate, k.tradeDate);
        }
        @Override public int hashCode() { return Objects.hash(backtestId, tradeDate); }
    }
}
