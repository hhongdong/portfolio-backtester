package com.portfolio.backtester.domain;

import com.portfolio.backtester.domain.execution.TransactionCostModel;
import com.portfolio.backtester.domain.market.MarketSnapshot;
import com.portfolio.backtester.domain.market.PriceBar;
import com.portfolio.backtester.domain.metrics.MetricsCalculator;
import com.portfolio.backtester.domain.metrics.PerformanceMetrics;
import com.portfolio.backtester.domain.portfolio.Portfolio;
import com.portfolio.backtester.domain.strategy.Strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core walk-forward simulation loop. Pure Java — no Spring, no JPA.
 *
 * Execution model on a rebalance day t:
 *   1. Strategy receives MarketSnapshot bounded to data dated < t.
 *      (No look-ahead: the strategy cannot see today's close because
 *       in real life that price would not be known when the order was sent.)
 *   2. Strategy emits target weights.
 *   3. Engine computes target dollar allocations using the OPENING price on
 *      day t. Trades are executed at open + half-spread (a conservative
 *      proxy for "we sent the order this morning at market").
 *   4. End-of-day portfolio value is marked using day-t close.
 */
public final class BacktestEngine {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public record Config(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal initialCapital,
            RebalancePeriod rebalancePeriod,
            BigDecimal annualRiskFreeRate,
            String benchmarkSymbol) {}

    private final Strategy strategy;
    private final TransactionCostModel costModel;
    private final PriceHistory history;
    private final Config config;

    public BacktestEngine(Strategy strategy,
                          TransactionCostModel costModel,
                          PriceHistory history,
                          Config config) {
        this.strategy = strategy;
        this.costModel = costModel;
        this.history = history;
        this.config = config;
    }

    public BacktestResult run() {
        Portfolio portfolio = new Portfolio(config.initialCapital());
        List<LocalDate> days = history.tradingDays(config.startDate(), config.endDate());
        if (days.isEmpty()) {
            throw new IllegalStateException("no trading days in range — is data ingested?");
        }

        List<BacktestResult.EquityPoint> equity = new ArrayList<>(days.size());
        List<BigDecimal> portValues = new ArrayList<>(days.size());
        List<BigDecimal> benchValues = new ArrayList<>(days.size());

        BigDecimal benchStartPrice = config.benchmarkSymbol() == null
                ? null
                : history.closeOn(config.benchmarkSymbol(), days.get(0))
                    .map(PriceBar::adjClose)
                    .orElse(null);

        LocalDate prevDay = null;
        for (int i = 0; i < days.size(); i++) {
            LocalDate today = days.get(i);
            boolean isRebalance = (prevDay == null)
                    || config.rebalancePeriod().shouldRebalance(prevDay, today);

            if (isRebalance) {
                MarketSnapshot snapshot = history.snapshotAt(today);
                Map<String, BigDecimal> targetWeights = strategy.targetWeights(snapshot);
                rebalance(portfolio, today, targetWeights);
            }

            // Mark to market on today's close
            Map<String, BigDecimal> closes = closesOn(today, portfolio.holdings().keySet());
            BigDecimal value = portfolio.value(closes);
            portValues.add(value);

            BigDecimal benchValue = null;
            if (benchStartPrice != null) {
                BigDecimal todayBench = history.closeOn(config.benchmarkSymbol(), today)
                        .map(PriceBar::adjClose)
                        .orElse(null);
                if (todayBench != null) {
                    benchValue = config.initialCapital()
                            .multiply(todayBench, MC)
                            .divide(benchStartPrice, 4, RoundingMode.HALF_UP);
                    benchValues.add(benchValue);
                }
            }
            equity.add(new BacktestResult.EquityPoint(today, value, benchValue));
            prevDay = today;
        }

        PerformanceMetrics metrics = MetricsCalculator.compute(
                portValues,
                benchValues.size() == portValues.size() ? benchValues : null,
                config.annualRiskFreeRate());

        return new BacktestResult(equity, portfolio.trades(), metrics);
    }

    private void rebalance(Portfolio portfolio, LocalDate today,
                           Map<String, BigDecimal> targetWeights) {
        Map<String, BigDecimal> execPrices = new HashMap<>();
        for (String symbol : targetWeights.keySet()) {
            history.closeOn(symbol, today).ifPresent(bar ->
                    execPrices.put(symbol, bar.open() != null ? bar.open() : bar.adjClose()));
        }
        // existing holdings need a price too (for value)
        for (String symbol : portfolio.holdings().keySet()) {
            if (!execPrices.containsKey(symbol)) {
                history.closeOn(symbol, today).ifPresent(bar ->
                        execPrices.put(symbol, bar.open() != null ? bar.open() : bar.adjClose()));
            }
        }

        BigDecimal totalValue = portfolio.value(execPrices);

        Set<String> allSymbols = new HashSet<>(targetWeights.keySet());
        allSymbols.addAll(portfolio.holdings().keySet());

        // Sells first to free up cash, then buys.
        for (String symbol : allSymbols) {
            BigDecimal price = execPrices.get(symbol);
            if (price == null || price.signum() == 0) continue;
            BigDecimal targetWeight = targetWeights.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal targetNotional = totalValue.multiply(targetWeight, MC);
            BigDecimal targetShares = targetNotional.divide(price, 8, RoundingMode.DOWN);
            BigDecimal currentShares = portfolio.holdings().getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal delta = targetShares.subtract(currentShares);
            if (delta.signum() < 0) {
                portfolio.executeSell(today, symbol, delta.abs(), price, costModel);
            }
        }
        for (String symbol : allSymbols) {
            BigDecimal price = execPrices.get(symbol);
            if (price == null || price.signum() == 0) continue;
            BigDecimal targetWeight = targetWeights.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal targetNotional = totalValue.multiply(targetWeight, MC);
            BigDecimal targetShares = targetNotional.divide(price, 8, RoundingMode.DOWN);
            BigDecimal currentShares = portfolio.holdings().getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal delta = targetShares.subtract(currentShares);
            if (delta.signum() > 0) {
                // Cap by available cash so we never go negative on rounding edges
                BigDecimal maxShares = portfolio.cash().divide(
                        price.add(costModel.costFor(price, com.portfolio.backtester.domain.portfolio.Trade.Side.BUY)),
                        8, RoundingMode.DOWN);
                BigDecimal qty = delta.min(maxShares.max(BigDecimal.ZERO));
                if (qty.signum() > 0) {
                    portfolio.executeBuy(today, symbol, qty, price, costModel);
                }
            }
        }
    }

    private Map<String, BigDecimal> closesOn(LocalDate date, Set<String> symbols) {
        Map<String, BigDecimal> out = new HashMap<>();
        for (String s : symbols) {
            history.closeOn(s, date).ifPresent(bar -> out.put(s, bar.adjClose()));
        }
        return out;
    }
}
