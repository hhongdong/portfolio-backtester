package com.portfolio.backtester.application.backtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backtester.api.dto.BacktestRequest;
import com.portfolio.backtester.application.data.PriceDataService;
import com.portfolio.backtester.domain.BacktestEngine;
import com.portfolio.backtester.domain.BacktestResult;
import com.portfolio.backtester.domain.PriceHistory;
import com.portfolio.backtester.domain.RebalancePeriod;
import com.portfolio.backtester.domain.execution.BpsCostModel;
import com.portfolio.backtester.domain.execution.TransactionCostModel;
import com.portfolio.backtester.domain.metrics.PerformanceMetrics;
import com.portfolio.backtester.domain.strategy.EqualWeightStrategy;
import com.portfolio.backtester.domain.strategy.Strategy;
import com.portfolio.backtester.infrastructure.persistence.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class BacktestService {

    private static final Logger log = LoggerFactory.getLogger(BacktestService.class);

    private final BacktestRunRepository runRepo;
    private final BacktestResultRepository resultRepo;
    private final EquityPointRepository equityRepo;
    private final TradeRepository tradeRepo;
    private final PriceDataService priceDataService;
    private final ObjectMapper objectMapper;
    private final Timer backtestTimer;

    public BacktestService(BacktestRunRepository runRepo,
                           BacktestResultRepository resultRepo,
                           EquityPointRepository equityRepo,
                           TradeRepository tradeRepo,
                           PriceDataService priceDataService,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.equityRepo = equityRepo;
        this.tradeRepo = tradeRepo;
        this.priceDataService = priceDataService;
        this.objectMapper = objectMapper;
        this.backtestTimer = Timer.builder("backtest.duration")
                .description("Wall-clock time to execute a backtest")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    /**
     * Submits a backtest for async execution. Idempotent: if a run with the
     * same canonical-input hash already exists, returns the existing run
     * instead of starting a new one.
     */
    @Transactional
    public BacktestRunEntity submit(BacktestRequest request) {
        String key = IdempotencyKey.of(request);
        return runRepo.findByIdempotencyKey(key).orElseGet(() -> {
            BacktestRunEntity run = new BacktestRunEntity();
            run.setId(UUID.randomUUID());
            run.setIdempotencyKey(key);
            run.setName(request.name());
            run.setStatus(BacktestRunEntity.Status.PENDING);
            try {
                run.setConfig(objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid config", e);
            }
            BacktestRunEntity saved = runRepo.save(run);
            executeAsync(saved.getId(), request);
            return saved;
        });
    }

    @Async("backtestExecutor")
    public void executeAsync(UUID runId, BacktestRequest request) {
        Timer.Sample sample = Timer.start();
        try {
            executeInTransaction(runId, request);
        } catch (Exception e) {
            log.error("backtest {} failed", runId, e);
            markFailed(runId, e.getMessage());
        } finally {
            sample.stop(backtestTimer);
        }
    }

    @Transactional
    public void executeInTransaction(UUID runId, BacktestRequest request) {
        BacktestRunEntity run = runRepo.findById(runId).orElseThrow();
        run.setStatus(BacktestRunEntity.Status.RUNNING);
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runRepo.save(run);

        BacktestEngine engine = buildEngine(request);
        BacktestResult result = engine.run();

        persist(runId, result);

        run.setStatus(BacktestRunEntity.Status.SUCCESS);
        run.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runRepo.save(run);
    }

    @Transactional
    public void markFailed(UUID runId, String message) {
        runRepo.findById(runId).ifPresent(run -> {
            run.setStatus(BacktestRunEntity.Status.FAILED);
            run.setErrorMessage(message);
            run.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            runRepo.save(run);
        });
    }

    private BacktestEngine buildEngine(BacktestRequest request) {
        List<String> universe = request.universe();
        String benchmark = request.benchmark();
        List<String> symbolsForLoad = benchmark == null
                ? universe
                : java.util.stream.Stream.concat(universe.stream(), java.util.stream.Stream.of(benchmark))
                        .distinct().toList();
        PriceHistory history = priceDataService.loadHistory(
                symbolsForLoad, request.startDate(), request.endDate());

        Strategy strategy = buildStrategy(request);
        TransactionCostModel costModel = buildCostModel(request);

        RebalancePeriod period = request.strategy().rebalancePeriod() == null
                ? RebalancePeriod.MONTHLY
                : RebalancePeriod.valueOf(request.strategy().rebalancePeriod().toUpperCase());

        BigDecimal rf = request.annualRiskFreeRate() == null
                ? new BigDecimal("0.02")
                : request.annualRiskFreeRate();

        BacktestEngine.Config config = new BacktestEngine.Config(
                request.startDate(),
                request.endDate(),
                request.initialCapital(),
                period,
                rf,
                benchmark);
        return new BacktestEngine(strategy, costModel, history, config);
    }

    private Strategy buildStrategy(BacktestRequest request) {
        String type = request.strategy().type().toUpperCase();
        return switch (type) {
            case "EQUAL_WEIGHT" -> new EqualWeightStrategy(request.universe());
            default -> throw new IllegalArgumentException("unknown strategy: " + type);
        };
    }

    private TransactionCostModel buildCostModel(BacktestRequest request) {
        BacktestRequest.ExecutionConfig exec = request.execution();
        BigDecimal commission = exec == null || exec.transactionCostBps() == null
                ? new BigDecimal("5")
                : exec.transactionCostBps();
        BigDecimal spread = exec == null || exec.bidAskSpreadBps() == null
                ? new BigDecimal("10")
                : exec.bidAskSpreadBps();
        return new BpsCostModel(commission, spread);
    }

    private void persist(UUID runId, BacktestResult result) {
        // equity curve
        List<EquityPointEntity> points = result.equityCurve().stream()
                .map(p -> new EquityPointEntity(runId, p.date(), p.portfolioValue(), p.benchmarkValue()))
                .toList();
        equityRepo.saveAll(points);

        // trades
        List<TradeEntity> trades = result.trades().stream()
                .map(t -> new TradeEntity(runId, t.date(), t.symbol(),
                        t.side().name(), t.quantity(), t.price(), t.cost()))
                .toList();
        tradeRepo.saveAll(trades);

        // metrics
        PerformanceMetrics m = result.metrics();
        BacktestResultEntity rEnt = new BacktestResultEntity();
        rEnt.setBacktestId(runId);
        rEnt.setTotalReturn(m.totalReturn());
        rEnt.setCagr(m.cagr());
        rEnt.setSharpe(m.sharpe());
        rEnt.setSortino(m.sortino());
        rEnt.setCalmar(m.calmar());
        rEnt.setMaxDrawdown(m.maxDrawdown());
        rEnt.setMaxDrawdownDurationDays(m.maxDrawdownDurationDays());
        rEnt.setVolatility(m.volatility());
        rEnt.setVar95(m.var95());
        rEnt.setCvar95(m.cvar95());
        rEnt.setBeta(m.beta());
        rEnt.setInformationRatio(m.informationRatio());
        resultRepo.save(rEnt);
    }
}
