package com.portfolio.backtester.infrastructure.external;

import com.portfolio.backtester.domain.market.PriceBar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates deterministic synthetic OHLCV bars so the backtester is
 * fully runnable without an Alpha Vantage / Yahoo API key. Each symbol
 * gets a unique drift+vol seeded from its name, so the data is stable
 * across runs (important for idempotency tests).
 *
 * Replace with {@link AlphaVantagePriceProvider} for real data.
 */
@Component
@ConditionalOnProperty(name = "backtester.ingestion.provider", havingValue = "stub", matchIfMissing = true)
public class StubPriceProvider implements PriceProvider {

    @Override
    public List<PriceBar> fetch(String symbol, LocalDate from, LocalDate to) {
        long seed = symbol.hashCode();
        Random rng = new Random(seed);
        // small idiosyncratic drift + vol per symbol
        double mu = 0.00025 + (rng.nextDouble() - 0.5) * 0.0005;   // ~6% annualized centered
        double sigma = 0.010 + rng.nextDouble() * 0.010;            // 16-32% annualized vol

        BigDecimal startPrice = BigDecimal.valueOf(50 + (Math.abs(seed) % 200));

        List<PriceBar> bars = new ArrayList<>();
        BigDecimal price = startPrice;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                double shock = mu + sigma * rng.nextGaussian();
                BigDecimal next = price.multiply(BigDecimal.valueOf(1.0 + shock))
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal open = price;
                BigDecimal close = next.max(BigDecimal.valueOf(0.01));
                BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.005))
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(0.995))
                        .setScale(4, RoundingMode.HALF_UP);
                long volume = 1_000_000L + rng.nextInt(5_000_000);
                bars.add(new PriceBar(symbol, d, open, high, low, close, close, volume));
                price = close;
            }
            d = d.plusDays(1);
        }
        return bars;
    }
}
