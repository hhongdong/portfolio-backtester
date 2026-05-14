package com.portfolio.backtester.domain.market;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The market as it would have appeared to a decision-maker AT a specific
 * date. The whole interface is structured so that look-ahead bias is
 * impossible by construction: every method bounds its return to data
 * dated strictly before {@link #asOf()}.
 *
 * Strategy code receives a snapshot and cannot reach past it. There is
 * deliberately no "give me everything" accessor — if you need history,
 * you must say how far back, and the snapshot guarantees you will not see
 * the current day's close (which would not have been known when you had
 * to place the order).
 */
public interface MarketSnapshot {

    /** The date the strategy is making decisions ON. */
    LocalDate asOf();

    /** The most recent close strictly before {@link #asOf()}. Empty if no
     *  prior data exists for that symbol. */
    Optional<PriceBar> latestBar(String symbol);

    /** All bars for {@code symbol} with date < {@link #asOf()}, oldest first,
     *  capped at {@code maxBars}. */
    List<PriceBar> history(String symbol, int maxBars);

    /** Universe of symbols currently listed (and not yet delisted) as of
     *  {@link #asOf()}. */
    List<String> activeUniverse();
}
