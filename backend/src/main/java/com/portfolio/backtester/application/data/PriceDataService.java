package com.portfolio.backtester.application.data;

import com.portfolio.backtester.domain.PriceHistory;
import com.portfolio.backtester.domain.market.PriceBar;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceEntity;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PriceDataService {

    private final DailyPriceRepository priceRepository;

    public PriceDataService(DailyPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /**
     * Loads the full price history for a universe over a date range.
     * Cached because identical backtest configurations replay the exact same
     * range; cache hit/miss counts are exposed via Micrometer.
     */
    @Cacheable(value = "priceRange", key = "T(java.util.Objects).hash(#symbols, #from, #to)")
    public PriceHistory loadHistory(List<String> symbols, LocalDate from, LocalDate to) {
        // Pull a generous tail before `from` so warm-up signals (e.g., 200-day MA
        // for momentum strategies) have data on day one.
        LocalDate fetchFrom = from.minusYears(1);
        List<DailyPriceEntity> rows = priceRepository.findRange(symbols, fetchFrom, to);
        List<PriceBar> bars = rows.stream().map(r -> new PriceBar(
                r.getSymbol(),
                r.getTradeDate(),
                r.getOpen(),
                r.getHigh(),
                r.getLow(),
                r.getClose(),
                r.getAdjClose(),
                r.getVolume() == null ? 0L : r.getVolume()
        )).toList();
        return PriceHistory.of(bars);
    }
}
