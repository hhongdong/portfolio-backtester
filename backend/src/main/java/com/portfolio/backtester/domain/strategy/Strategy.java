package com.portfolio.backtester.domain.strategy;

import com.portfolio.backtester.domain.market.MarketSnapshot;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A strategy emits target portfolio weights given a point-in-time market
 * snapshot. The runner is responsible for translating weights into trades.
 *
 * Strategy implementations MUST be deterministic given the same snapshot
 * — replay tests depend on this.
 */
public interface Strategy {

    /** Stable identifier used in idempotency keys and logs. */
    String name();

    /**
     * Returns target weights summing to <= 1.0 (any remainder stays in cash).
     * Symbols not in the map are treated as 0% allocation.
     */
    Map<String, BigDecimal> targetWeights(MarketSnapshot snapshot);
}
