package com.portfolio.backtester.config;

import com.portfolio.backtester.application.data.PriceIngestionService;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupSeeder {

    private static final Logger log = LoggerFactory.getLogger(StartupSeeder.class);

    /**
     * On first boot, the daily_prices table is empty — seed it so the
     * /backtests endpoint actually has data to play against. Subsequent
     * starts skip this because rows already exist.
     */
    @Bean
    ApplicationRunner seedPricesIfEmpty(DailyPriceRepository priceRepository,
                                        PriceIngestionService ingestionService) {
        return args -> {
            if (priceRepository.count() == 0) {
                log.info("daily_prices is empty — seeding from configured provider");
                int n = ingestionService.refreshUniverse();
                log.info("Seeded {} bars", n);
            }
        };
    }
}
