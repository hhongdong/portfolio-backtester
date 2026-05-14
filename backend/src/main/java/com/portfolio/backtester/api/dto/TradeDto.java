package com.portfolio.backtester.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeDto(LocalDate date,
                       String symbol,
                       String side,
                       BigDecimal quantity,
                       BigDecimal price,
                       BigDecimal cost) {}
