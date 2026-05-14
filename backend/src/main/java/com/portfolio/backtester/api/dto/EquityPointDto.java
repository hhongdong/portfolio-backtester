package com.portfolio.backtester.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EquityPointDto(LocalDate date,
                             BigDecimal portfolioValue,
                             BigDecimal benchmarkValue) {}
