package com.portfolio.backtester.domain.execution;

import com.portfolio.backtester.domain.portfolio.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Linear cost model: commission in basis points + half the bid-ask spread
 * applied on each side of the trade. 1 bp = 0.01%.
 */
public final class BpsCostModel implements TransactionCostModel {

    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final BigDecimal commissionBps;
    private final BigDecimal spreadBps;

    public BpsCostModel(BigDecimal commissionBps, BigDecimal spreadBps) {
        this.commissionBps = commissionBps;
        this.spreadBps = spreadBps;
    }

    @Override
    public BigDecimal costFor(BigDecimal grossNotional, Trade.Side side) {
        BigDecimal commission = grossNotional
                .multiply(commissionBps)
                .divide(TEN_THOUSAND, 8, RoundingMode.HALF_UP);
        BigDecimal halfSpread = grossNotional
                .multiply(spreadBps)
                .divide(TEN_THOUSAND, 8, RoundingMode.HALF_UP)
                .divide(TWO, 8, RoundingMode.HALF_UP);
        return commission.add(halfSpread);
    }
}
