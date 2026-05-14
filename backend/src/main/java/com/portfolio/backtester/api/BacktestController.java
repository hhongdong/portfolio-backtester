package com.portfolio.backtester.api;

import com.portfolio.backtester.api.dto.BacktestRequest;
import com.portfolio.backtester.api.dto.BacktestResponse;
import com.portfolio.backtester.api.dto.EquityPointDto;
import com.portfolio.backtester.api.dto.TradeDto;
import com.portfolio.backtester.application.backtest.BacktestService;
import com.portfolio.backtester.infrastructure.persistence.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backtests")
@Tag(name = "Backtests", description = "Submit and inspect backtests")
public class BacktestController {

    private final BacktestService backtestService;
    private final BacktestRunRepository runRepo;
    private final BacktestResultRepository resultRepo;
    private final EquityPointRepository equityRepo;
    private final TradeRepository tradeRepo;

    public BacktestController(BacktestService backtestService,
                              BacktestRunRepository runRepo,
                              BacktestResultRepository resultRepo,
                              EquityPointRepository equityRepo,
                              TradeRepository tradeRepo) {
        this.backtestService = backtestService;
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.equityRepo = equityRepo;
        this.tradeRepo = tradeRepo;
    }

    @Operation(summary = "Submit a backtest. Returns 202 Accepted with a job ID.")
    @PostMapping
    public ResponseEntity<BacktestResponse> submit(@Valid @RequestBody BacktestRequest request) {
        BacktestRunEntity run = backtestService.submit(request);
        BacktestResponse body = toResponse(run, null);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/backtests/" + run.getId()))
                .body(body);
    }

    @Operation(summary = "Get a backtest by ID, including metrics if completed.")
    @GetMapping("/{id}")
    public ResponseEntity<BacktestResponse> get(@PathVariable UUID id) {
        return runRepo.findById(id)
                .map(run -> {
                    BacktestResultEntity result = resultRepo.findById(id).orElse(null);
                    return ResponseEntity.ok(toResponse(run, result));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/equity")
    public ResponseEntity<List<EquityPointDto>> equity(@PathVariable UUID id) {
        List<EquityPointDto> points = equityRepo.findByBacktestId(id).stream()
                .map(e -> new EquityPointDto(e.getTradeDate(), e.getPortfolioValue(), e.getBenchmarkValue()))
                .toList();
        return ResponseEntity.ok(points);
    }

    @GetMapping("/{id}/trades")
    public ResponseEntity<Page<TradeDto>> trades(@PathVariable UUID id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "100") int size) {
        Page<TradeDto> trades = tradeRepo.findByBacktestIdOrderByTradeDateAsc(
                id, PageRequest.of(page, size))
                .map(t -> new TradeDto(t.getTradeDate(), t.getSymbol(), t.getSide(),
                        t.getQuantity(), t.getPrice(), t.getCost()));
        return ResponseEntity.ok(trades);
    }

    @GetMapping
    public Page<BacktestResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        Page<BacktestRunEntity> runs = runRepo.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return runs.map(run -> {
            BacktestResultEntity result = resultRepo.findById(run.getId()).orElse(null);
            return toResponse(run, result);
        });
    }

    private BacktestResponse toResponse(BacktestRunEntity run, BacktestResultEntity r) {
        BacktestResponse.Metrics metrics = r == null ? null : new BacktestResponse.Metrics(
                r.getTotalReturn(), r.getCagr(), r.getSharpe(), r.getSortino(), r.getCalmar(),
                r.getMaxDrawdown(), r.getMaxDrawdownDurationDays(), r.getVolatility(),
                r.getVar95(), r.getCvar95(), r.getBeta(), r.getInformationRatio());
        return new BacktestResponse(
                run.getId(),
                run.getName(),
                run.getStatus().name(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorMessage(),
                metrics);
    }
}
