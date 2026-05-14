package com.portfolio.backtester.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day of OHLCV data for one symbol. Adjusted close already accounts for
 * splits and dividends — strategies should use it for return calculations.
 */
public record PriceBar(
        String symbol,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjClose,
        long volume) {
}
