package com.portfolio.backtester.domain.metrics;

import java.math.BigDecimal;

/**
 * Aggregate of risk-adjusted return metrics for a backtest. All ratios are
 * annualized assuming 252 trading days per year. Drawdowns are expressed as
 * negative fractions (e.g., -0.20 = a 20% peak-to-trough loss).
 */
public record PerformanceMetrics(
        BigDecimal totalReturn,
        BigDecimal cagr,
        BigDecimal sharpe,
        BigDecimal sortino,
        BigDecimal calmar,
        BigDecimal maxDrawdown,
        int maxDrawdownDurationDays,
        BigDecimal volatility,
        BigDecimal var95,
        BigDecimal cvar95,
        BigDecimal beta,
        BigDecimal informationRatio) {
}
