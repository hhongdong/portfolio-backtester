package com.portfolio.backtester.domain;

import com.portfolio.backtester.domain.market.MarketSnapshot;
import com.portfolio.backtester.domain.market.PriceBar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Indexed in-memory price history for the entire backtest universe. Built
 * once at the start of a run; used to construct a sequence of
 * {@link MarketSnapshot}s as the engine walks the calendar.
 *
 * THE LOOK-AHEAD GUARANTEE: snapshots returned by {@link #snapshotAt(LocalDate)}
 * cannot expose any bar dated >= the snapshot date. The cap is enforced
 * here, not in strategy code, so individual strategies cannot accidentally
 * peek into the future.
 */
public final class PriceHistory {

    private final Map<String, TreeMap<LocalDate, PriceBar>> bySymbol;
    private final TreeMap<LocalDate, List<PriceBar>> byDate;

    private PriceHistory(Map<String, TreeMap<LocalDate, PriceBar>> bySymbol,
                         TreeMap<LocalDate, List<PriceBar>> byDate) {
        this.bySymbol = bySymbol;
        this.byDate = byDate;
    }

    public static PriceHistory of(List<PriceBar> bars) {
        Map<String, TreeMap<LocalDate, PriceBar>> bySymbol = new HashMap<>();
        TreeMap<LocalDate, List<PriceBar>> byDate = new TreeMap<>();
        for (PriceBar bar : bars) {
            bySymbol.computeIfAbsent(bar.symbol(), k -> new TreeMap<>()).put(bar.date(), bar);
            byDate.computeIfAbsent(bar.date(), k -> new ArrayList<>()).add(bar);
        }
        return new PriceHistory(bySymbol, byDate);
    }

    public List<LocalDate> tradingDays(LocalDate from, LocalDate to) {
        return new ArrayList<>(byDate.subMap(from, true, to, true).keySet());
    }

    /** Most recent close ON or BEFORE {@code date} for {@code symbol}. */
    public Optional<PriceBar> closeOn(String symbol, LocalDate date) {
        TreeMap<LocalDate, PriceBar> series = bySymbol.get(symbol);
        if (series == null) return Optional.empty();
        Map.Entry<LocalDate, PriceBar> e = series.floorEntry(date);
        return Optional.ofNullable(e == null ? null : e.getValue());
    }

    public MarketSnapshot snapshotAt(LocalDate asOf) {
        return new BoundedSnapshot(asOf);
    }

    private final class BoundedSnapshot implements MarketSnapshot {
        private final LocalDate asOf;

        BoundedSnapshot(LocalDate asOf) { this.asOf = asOf; }

        @Override public LocalDate asOf() { return asOf; }

        @Override
        public Optional<PriceBar> latestBar(String symbol) {
            TreeMap<LocalDate, PriceBar> series = bySymbol.get(symbol);
            if (series == null) return Optional.empty();
            // strict upper bound — never expose a bar dated >= asOf
            Map.Entry<LocalDate, PriceBar> e = series.lowerEntry(asOf);
            return Optional.ofNullable(e == null ? null : e.getValue());
        }

        @Override
        public List<PriceBar> history(String symbol, int maxBars) {
            TreeMap<LocalDate, PriceBar> series = bySymbol.get(symbol);
            if (series == null) return Collections.emptyList();
            var head = series.headMap(asOf, false);
            List<PriceBar> all = new ArrayList<>(head.values());
            int from = Math.max(0, all.size() - maxBars);
            return all.subList(from, all.size());
        }

        @Override
        public List<String> activeUniverse() {
            List<String> out = new ArrayList<>();
            for (var entry : bySymbol.entrySet()) {
                if (entry.getValue().lowerKey(asOf) != null) out.add(entry.getKey());
            }
            Collections.sort(out);
            return out;
        }
    }
}
