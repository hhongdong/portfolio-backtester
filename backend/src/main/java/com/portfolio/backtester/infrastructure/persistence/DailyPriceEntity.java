package com.portfolio.backtester.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "daily_prices")
public class DailyPriceEntity {

    @EmbeddedId
    private Key id;

    @Column private BigDecimal open;
    @Column private BigDecimal high;
    @Column private BigDecimal low;
    @Column(nullable = false) private BigDecimal close;
    @Column(name = "adj_close", nullable = false) private BigDecimal adjClose;
    @Column private Long volume;

    public DailyPriceEntity() {}

    public DailyPriceEntity(String symbol, LocalDate date, BigDecimal open, BigDecimal high,
                            BigDecimal low, BigDecimal close, BigDecimal adjClose, Long volume) {
        this.id = new Key(symbol, date);
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjClose = adjClose;
        this.volume = volume;
    }

    public String getSymbol() { return id.symbol; }
    public LocalDate getTradeDate() { return id.tradeDate; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public BigDecimal getAdjClose() { return adjClose; }
    public Long getVolume() { return volume; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(nullable = false) private String symbol;
        @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;

        public Key() {}
        public Key(String symbol, LocalDate tradeDate) {
            this.symbol = symbol;
            this.tradeDate = tradeDate;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(symbol, k.symbol) && Objects.equals(tradeDate, k.tradeDate);
        }
        @Override public int hashCode() { return Objects.hash(symbol, tradeDate); }
    }
}
