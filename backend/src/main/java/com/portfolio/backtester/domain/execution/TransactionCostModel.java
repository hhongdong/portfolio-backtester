package com.portfolio.backtester.domain.execution;

import com.portfolio.backtester.domain.portfolio.Trade;

import java.math.BigDecimal;

/**
 * Cost a strategy pays per trade. Implementations model commissions,
 * bid-ask spreads, and (optionally) volume-based slippage.
 */
public interface TransactionCostModel {

    /**
     * Total cost (commission + half-spread + any slippage) on a trade
     * with the given gross notional and side.
     */
    BigDecimal costFor(BigDecimal grossNotional, Trade.Side side);
}
