package com.portfolio.backtester.application.data;

import com.portfolio.backtester.domain.market.PriceBar;
import com.portfolio.backtester.infrastructure.external.PriceProvider;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceEntity;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceRepository;
import com.portfolio.backtester.infrastructure.persistence.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PriceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PriceIngestionService.class);

    private final SymbolRepository symbolRepository;
    private final DailyPriceRepository priceRepository;
    private final PriceProvider provider;
    private final CacheManager cacheManager;

    public PriceIngestionService(SymbolRepository symbolRepository,
                                 DailyPriceRepository priceRepository,
                                 PriceProvider provider,
                                 CacheManager cacheManager) {
        this.symbolRepository = symbolRepository;
        this.priceRepository = priceRepository;
        this.provider = provider;
        this.cacheManager = cacheManager;
    }

    /** Refresh every symbol in the universe. Returns total bars upserted. */
    @Transactional
    public int refreshUniverse() {
        int total = 0;
        for (var sym : symbolRepository.findAll()) {
            total += refreshSymbol(sym.getSymbol());
        }
        invalidateCaches();
        return total;
    }

    @Transactional
    public int refreshSymbol(String symbol) {
        LocalDate from = priceRepository.latestDateFor(symbol)
                .map(d -> d.plusDays(1))
                .orElse(LocalDate.now().minusYears(10));
        LocalDate to = LocalDate.now();
        if (!from.isBefore(to)) {
            return 0;
        }
        List<PriceBar> bars = provider.fetch(symbol, from, to);
        for (PriceBar b : bars) {
            priceRepository.save(new DailyPriceEntity(
                    b.symbol(), b.date(), b.open(), b.high(), b.low(),
                    b.close(), b.adjClose(), b.volume()));
        }
        log.info("Ingested {} bars for {}", bars.size(), symbol);
        return bars.size();
    }

    /** Nightly refresh — runs at 02:00 UTC after US markets close. */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void nightlyRefresh() {
        log.info("Nightly price refresh starting");
        int n = refreshUniverse();
        log.info("Nightly price refresh complete: {} bars", n);
    }

    private void invalidateCaches() {
        var cache = cacheManager.getCache("priceRange");
        if (cache != null) cache.clear();
    }
}
