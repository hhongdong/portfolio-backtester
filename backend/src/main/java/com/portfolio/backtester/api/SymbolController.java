package com.portfolio.backtester.api;

import com.portfolio.backtester.api.dto.EquityPointDto;
import com.portfolio.backtester.infrastructure.persistence.DailyPriceRepository;
import com.portfolio.backtester.infrastructure.persistence.SymbolEntity;
import com.portfolio.backtester.infrastructure.persistence.SymbolRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/symbols")
@Tag(name = "Symbols", description = "Universe browser and price history")
public class SymbolController {

    private final SymbolRepository symbolRepository;
    private final DailyPriceRepository priceRepository;

    public SymbolController(SymbolRepository symbolRepository,
                            DailyPriceRepository priceRepository) {
        this.symbolRepository = symbolRepository;
        this.priceRepository = priceRepository;
    }

    public record SymbolDto(String symbol, String name, String sector,
                            LocalDate listedAt, LocalDate delistedAt) {}

    public record PriceDto(LocalDate date, BigDecimal open, BigDecimal high,
                           BigDecimal low, BigDecimal close, BigDecimal adjClose,
                           Long volume) {}

    @GetMapping
    public List<SymbolDto> list() {
        return symbolRepository.findAll().stream()
                .map(s -> new SymbolDto(s.getSymbol(), s.getName(), s.getSector(),
                        s.getListedAt(), s.getDelistedAt()))
                .toList();
    }

    @GetMapping("/{symbol}/prices")
    public List<PriceDto> prices(@PathVariable String symbol,
                                 @RequestParam LocalDate from,
                                 @RequestParam LocalDate to) {
        return priceRepository.findForSymbol(symbol, from, to).stream()
                .map(p -> new PriceDto(p.getTradeDate(), p.getOpen(), p.getHigh(),
                        p.getLow(), p.getClose(), p.getAdjClose(), p.getVolume()))
                .toList();
    }
}
