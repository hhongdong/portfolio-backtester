package com.portfolio.backtester.domain.portfolio;

import com.portfolio.backtester.domain.execution.TransactionCostModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable portfolio aggregate. Tracks cash, holdings (symbol -> shares) and
 * a running trade log. All quantities are {@link BigDecimal} — fractional
 * shares allowed so equal-weight portfolios don't have to round and lose money.
 */
public final class Portfolio {

    private static final int SCALE = 8;

    private BigDecimal cash;
    private final Map<String, BigDecimal> holdings = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    public Portfolio(BigDecimal initialCash) {
        this.cash = initialCash;
    }

    public BigDecimal cash() {
        return cash;
    }

    public Map<String, BigDecimal> holdings() {
        return Collections.unmodifiableMap(holdings);
    }

    public List<Trade> trades() {
        return Collections.unmodifiableList(trades);
    }

    /** Total mark-to-market value at the given prices. */
    public BigDecimal value(Map<String, BigDecimal> prices) {
        BigDecimal v = cash;
        for (var entry : holdings.entrySet()) {
            BigDecimal price = prices.get(entry.getKey());
            if (price == null) continue;
            v = v.add(price.multiply(entry.getValue()));
        }
        return v;
    }

    public void executeBuy(LocalDate date, String symbol, BigDecimal quantity,
                           BigDecimal price, TransactionCostModel costModel) {
        if (quantity.signum() <= 0) return;
        BigDecimal notional = quantity.multiply(price);
        BigDecimal cost = costModel.costFor(notional, Trade.Side.BUY);
        BigDecimal totalDebit = notional.add(cost);
        cash = cash.subtract(totalDebit);
        holdings.merge(symbol, quantity, BigDecimal::add);
        trades.add(new Trade(date, symbol, Trade.Side.BUY, quantity, price, cost));
    }

    public void executeSell(LocalDate date, String symbol, BigDecimal quantity,
                            BigDecimal price, TransactionCostModel costModel) {
        if (quantity.signum() <= 0) return;
        BigDecimal held = holdings.getOrDefault(symbol, BigDecimal.ZERO);
        BigDecimal toSell = quantity.min(held);
        if (toSell.signum() <= 0) return;
        BigDecimal notional = toSell.multiply(price);
        BigDecimal cost = costModel.costFor(notional, Trade.Side.SELL);
        cash = cash.add(notional).subtract(cost);
        BigDecimal remaining = held.subtract(toSell);
        if (remaining.signum() <= 0) {
            holdings.remove(symbol);
        } else {
            holdings.put(symbol, remaining.setScale(SCALE, RoundingMode.HALF_UP));
        }
        trades.add(new Trade(date, symbol, Trade.Side.SELL, toSell, price, cost));
    }
}
