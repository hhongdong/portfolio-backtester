package com.portfolio.backtester.domain.strategy;

import com.portfolio.backtester.domain.market.MarketSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allocates equal weight across every symbol in the configured universe
 * that has at least one prior price bar. Symbols with no history are
 * skipped (e.g., tickers that hadn't IPO'd yet at the snapshot date).
 */
public final class EqualWeightStrategy implements Strategy {

    private final List<String> universe;

    public EqualWeightStrategy(List<String> universe) {
        if (universe == null || universe.isEmpty()) {
            throw new IllegalArgumentException("universe must be non-empty");
        }
        this.universe = List.copyOf(universe);
    }

    @Override
    public String name() {
        return "EQUAL_WEIGHT";
    }

    @Override
    public Map<String, BigDecimal> targetWeights(MarketSnapshot snapshot) {
        List<String> investable = universe.stream()
                .filter(s -> snapshot.latestBar(s).isPresent())
                .toList();
        if (investable.isEmpty()) {
            return Map.of();
        }
        BigDecimal w = BigDecimal.ONE.divide(
                BigDecimal.valueOf(investable.size()), 8, RoundingMode.HALF_DOWN);
        Map<String, BigDecimal> weights = new HashMap<>();
        for (String s : investable) weights.put(s, w);
        return weights;
    }
}
