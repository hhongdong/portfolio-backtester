package com.portfolio.backtester.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Trade(
        LocalDate date,
        String symbol,
        Side side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal cost) {

    public enum Side { BUY, SELL }

    public BigDecimal grossNotional() {
        return quantity.multiply(price);
    }
}
