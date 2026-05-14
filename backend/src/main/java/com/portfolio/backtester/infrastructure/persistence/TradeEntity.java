package com.portfolio.backtester.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "backtest_trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backtest_id", nullable = false)
    private UUID backtestId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String side;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal cost;

    public TradeEntity() {}

    public TradeEntity(UUID backtestId, LocalDate tradeDate, String symbol, String side,
                       BigDecimal quantity, BigDecimal price, BigDecimal cost) {
        this.backtestId = backtestId;
        this.tradeDate = tradeDate;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.cost = cost;
    }

    public Long getId() { return id; }
    public UUID getBacktestId() { return backtestId; }
    public LocalDate getTradeDate() { return tradeDate; }
    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCost() { return cost; }
}
