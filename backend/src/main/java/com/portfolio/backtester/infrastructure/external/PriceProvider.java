package com.portfolio.backtester.infrastructure.external;

import com.portfolio.backtester.domain.market.PriceBar;

import java.time.LocalDate;
import java.util.List;

public interface PriceProvider {
    List<PriceBar> fetch(String symbol, LocalDate from, LocalDate to);
}
