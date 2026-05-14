package com.portfolio.backtester.domain.strategy;

import com.portfolio.backtester.domain.market.MarketSnapshot;
import com.portfolio.backtester.domain.market.PriceBar;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EqualWeightStrategyTest {

    @Test
    void weightsSumToOne_acrossInvestableUniverse() {
        var strategy = new EqualWeightStrategy(List.of("A", "B", "C", "D"));
        var snap = snapshotWith("A", "B", "C", "D"); // all have history
        Map<String, BigDecimal> weights = strategy.targetWeights(snap);
        assertThat(weights).hasSize(4);
        BigDecimal sum = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum.doubleValue()).isCloseTo(1.0, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    void skipsSymbolsWithoutPriorData() {
        // Only A and B have history; C and D return Optional.empty()
        var strategy = new EqualWeightStrategy(List.of("A", "B", "C", "D"));
        var snap = snapshotWith("A", "B");
        Map<String, BigDecimal> weights = strategy.targetWeights(snap);
        assertThat(weights).containsOnlyKeys("A", "B");
        weights.values().forEach(w ->
                assertThat(w.doubleValue()).isCloseTo(0.5, org.assertj.core.api.Assertions.within(0.0001)));
    }

    private static MarketSnapshot snapshotWith(String... withHistory) {
        var present = java.util.Set.of(withHistory);
        return new MarketSnapshot() {
            @Override public LocalDate asOf() { return LocalDate.of(2020, 1, 1); }
            @Override public Optional<PriceBar> latestBar(String symbol) {
                if (!present.contains(symbol)) return Optional.empty();
                return Optional.of(new PriceBar(symbol, LocalDate.of(2019, 12, 31),
                        new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
                        new BigDecimal("100"), new BigDecimal("100"), 1L));
            }
            @Override public List<PriceBar> history(String symbol, int maxBars) { return List.of(); }
            @Override public List<String> activeUniverse() { return List.copyOf(present); }
        };
    }
}
