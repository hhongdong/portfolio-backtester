package com.portfolio.backtester.infrastructure.external;

import com.portfolio.backtester.domain.market.PriceBar;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Alpha Vantage TIME_SERIES_DAILY_ADJUSTED. Wrapped with Resilience4j retry
 * + circuit breaker so transient 429s / 5xxs don't fail the whole ingest.
 *
 * Caveat: Alpha Vantage free tier deprecated _ADJUSTED. Use a paid plan, or
 * use the stub provider for demos.
 */
@Component
@ConditionalOnProperty(name = "backtester.ingestion.provider", havingValue = "alpha-vantage")
public class AlphaVantagePriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantagePriceProvider.class);

    private final WebClient client;
    private final String apiKey;

    public AlphaVantagePriceProvider(
            @Value("${backtester.ingestion.alpha-vantage.base-url}") String baseUrl,
            @Value("${backtester.ingestion.alpha-vantage.api-key}") String apiKey) {
        this.client = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    @Retry(name = "priceData")
    @CircuitBreaker(name = "priceData")
    @SuppressWarnings("unchecked")
    public List<PriceBar> fetch(String symbol, LocalDate from, LocalDate to) {
        Map<String, Object> response = client.get()
                .uri(uri -> uri.path("/query")
                        .queryParam("function", "TIME_SERIES_DAILY_ADJUSTED")
                        .queryParam("symbol", symbol)
                        .queryParam("outputsize", "full")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (response == null || !response.containsKey("Time Series (Daily)")) {
            log.warn("No data for {} — response: {}", symbol, response);
            return List.of();
        }
        Map<String, Map<String, String>> series =
                (Map<String, Map<String, String>>) response.get("Time Series (Daily)");

        List<PriceBar> out = new ArrayList<>();
        for (var entry : series.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey());
            if (date.isBefore(from) || date.isAfter(to)) continue;
            Map<String, String> v = entry.getValue();
            out.add(new PriceBar(
                    symbol,
                    date,
                    new BigDecimal(v.get("1. open")),
                    new BigDecimal(v.get("2. high")),
                    new BigDecimal(v.get("3. low")),
                    new BigDecimal(v.get("4. close")),
                    new BigDecimal(v.get("5. adjusted close")),
                    Long.parseLong(v.get("6. volume"))));
        }
        return out;
    }
}
