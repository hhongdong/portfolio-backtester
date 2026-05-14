package com.portfolio.backtester.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BacktestRequest(
        String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("1.00") BigDecimal initialCapital,
        @NotEmpty List<String> universe,
        @NotNull @Valid StrategyConfig strategy,
        ExecutionConfig execution,
        WalkForwardConfig walkForward,
        String benchmark,
        BigDecimal annualRiskFreeRate) {

    public record StrategyConfig(
            @NotNull String type,
            String rebalancePeriod) {}

    public record ExecutionConfig(
            BigDecimal transactionCostBps,
            BigDecimal bidAskSpreadBps,
            String slippageModel) {}

    public record WalkForwardConfig(
            Boolean enabled,
            Integer trainingWindowMonths,
            Integer testingWindowMonths) {}
}
