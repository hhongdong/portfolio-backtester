package com.portfolio.backtester.api;

import com.portfolio.backtester.application.data.PriceIngestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
@Tag(name = "Ingestion", description = "Manual triggers for price-data ingestion")
public class IngestionController {

    private final PriceIngestionService ingestionService;

    public IngestionController(PriceIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/trigger")
    public Map<String, Object> trigger(@RequestParam(required = false) String symbol) {
        int count = symbol == null
                ? ingestionService.refreshUniverse()
                : ingestionService.refreshSymbol(symbol);
        return Map.of("ingested", count, "symbol", symbol == null ? "ALL" : symbol);
    }
}
