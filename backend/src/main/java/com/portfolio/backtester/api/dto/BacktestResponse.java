package com.portfolio.backtester.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BacktestResponse(
        UUID id,
        String name,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String errorMessage,
        Metrics metrics) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Metrics(
            BigDecimal totalReturn,
            BigDecimal cagr,
            BigDecimal sharpe,
            BigDecimal sortino,
            BigDecimal calmar,
            BigDecimal maxDrawdown,
            Integer maxDrawdownDurationDays,
            BigDecimal volatility,
            BigDecimal var95,
            BigDecimal cvar95,
            BigDecimal beta,
            BigDecimal informationRatio) {}
}
