package com.portfolio.backtester.domain;

import com.portfolio.backtester.domain.metrics.PerformanceMetrics;
import com.portfolio.backtester.domain.portfolio.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResult(
        List<EquityPoint> equityCurve,
        List<Trade> trades,
        PerformanceMetrics metrics) {

    public record EquityPoint(LocalDate date, BigDecimal portfolioValue, BigDecimal benchmarkValue) {}
}
