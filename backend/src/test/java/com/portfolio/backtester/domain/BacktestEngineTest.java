package com.portfolio.backtester.domain;

import com.portfolio.backtester.domain.execution.BpsCostModel;
import com.portfolio.backtester.domain.market.PriceBar;
import com.portfolio.backtester.domain.strategy.EqualWeightStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end domain test of the backtest engine — pure JUnit, no Spring.
 * Demonstrates that the engine can replay a full year and produce coherent
 * metrics on synthetic data.
 */
class BacktestEngineTest {

    @Test
    void equalWeight_overOneYear_producesFiniteMetrics() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2020, 12, 31);

        List<PriceBar> bars = new ArrayList<>();
        bars.addAll(generateSeries("AAPL", start, end, 100.0, 0.0005));
        bars.addAll(generateSeries("MSFT", start, end, 200.0, 0.0003));
        bars.addAll(generateSeries("SPY",  start, end, 300.0, 0.0004));

        var history = PriceHistory.of(bars);

        var engine = new BacktestEngine(
                new EqualWeightStrategy(List.of("AAPL", "MSFT")),
                new BpsCostModel(new BigDecimal("5"), new BigDecimal("10")),
                history,
                new BacktestEngine.Config(
                        start, end,
                        new BigDecimal("100000"),
                        RebalancePeriod.QUARTERLY,
                        new BigDecimal("0.02"),
                        "SPY"));

        var result = engine.run();

        assertThat(result.equityCurve()).isNotEmpty();
        assertThat(result.metrics().totalReturn()).isNotNull();
        assertThat(result.metrics().sharpe()).isNotNull();
        // We rebalanced 4 times (quarterly) with 2 symbols -> at least 2 trades
        assertThat(result.trades().size()).isGreaterThanOrEqualTo(2);
    }

    private static List<PriceBar> generateSeries(String symbol, LocalDate from, LocalDate to,
                                                 double startPrice, double driftPerDay) {
        List<PriceBar> bars = new ArrayList<>();
        BigDecimal price = BigDecimal.valueOf(startPrice);
        LocalDate d = from;
        int step = 0;
        while (!d.isAfter(to)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                double signal = Math.sin(step / 20.0) * 0.005;
                price = price.multiply(BigDecimal.valueOf(1.0 + driftPerDay + signal));
                price = price.setScale(4, java.math.RoundingMode.HALF_UP);
                bars.add(new PriceBar(symbol, d, price, price, price, price, price, 1_000_000L));
                step++;
            }
            d = d.plusDays(1);
        }
        return bars;
    }
}
