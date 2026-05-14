package com.portfolio.backtester.domain;

import com.portfolio.backtester.domain.market.PriceBar;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single most important architectural test in the project: the
 * MarketSnapshot CANNOT see today's bar. If a regression here lets future
 * data leak into a strategy, all backtest results become suspect.
 */
class PriceHistoryTest {

    @Test
    void snapshot_strictlyExcludesAsOfDate() {
        LocalDate jan1 = LocalDate.of(2020, 1, 1);
        LocalDate jan2 = LocalDate.of(2020, 1, 2);
        LocalDate jan3 = LocalDate.of(2020, 1, 3);

        var history = PriceHistory.of(List.of(
                bar("AAPL", jan1, "100"),
                bar("AAPL", jan2, "101"),
                bar("AAPL", jan3, "102")
        ));

        var snap = history.snapshotAt(jan2);
        // The latest bar visible to a strategy on jan2 must be jan1, NEVER jan2 itself
        assertThat(snap.latestBar("AAPL")).isPresent();
        assertThat(snap.latestBar("AAPL").get().date()).isEqualTo(jan1);
    }

    @Test
    void snapshot_history_capsAtAsOfDate() {
        LocalDate d1 = LocalDate.of(2020, 1, 1);
        LocalDate d2 = LocalDate.of(2020, 1, 2);
        LocalDate d3 = LocalDate.of(2020, 1, 3);
        LocalDate d4 = LocalDate.of(2020, 1, 4);

        var history = PriceHistory.of(List.of(
                bar("X", d1, "10"), bar("X", d2, "11"),
                bar("X", d3, "12"), bar("X", d4, "13")));

        var snap = history.snapshotAt(d3);
        var hist = snap.history("X", 100);
        assertThat(hist).hasSize(2);
        assertThat(hist.get(hist.size() - 1).date()).isEqualTo(d2);
    }

    @Test
    void snapshot_returnsEmptyForUnknownSymbol() {
        var history = PriceHistory.of(List.of(
                bar("A", LocalDate.of(2020, 1, 1), "10")));
        assertThat(history.snapshotAt(LocalDate.of(2020, 1, 2)).latestBar("UNKNOWN")).isEmpty();
    }

    private static PriceBar bar(String symbol, LocalDate date, String px) {
        var p = new BigDecimal(px);
        return new PriceBar(symbol, date, p, p, p, p, p, 1_000L);
    }
}
