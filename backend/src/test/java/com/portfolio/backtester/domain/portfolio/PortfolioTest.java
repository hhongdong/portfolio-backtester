package com.portfolio.backtester.domain.portfolio;

import com.portfolio.backtester.domain.execution.BpsCostModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PortfolioTest {

    private final LocalDate today = LocalDate.of(2020, 1, 2);
    private final BpsCostModel zeroCost = new BpsCostModel(BigDecimal.ZERO, BigDecimal.ZERO);

    @Test
    void buy_decreasesCash_increasesHolding() {
        var p = new Portfolio(new BigDecimal("10000"));
        p.executeBuy(today, "AAPL", new BigDecimal("10"), new BigDecimal("100"), zeroCost);

        assertThat(p.cash().doubleValue()).isCloseTo(9000.0, within(0.0001));
        assertThat(p.holdings().get("AAPL")).isEqualByComparingTo("10");
    }

    @Test
    void sell_increasesCash_decreasesHolding() {
        var p = new Portfolio(new BigDecimal("10000"));
        p.executeBuy(today, "AAPL", new BigDecimal("10"), new BigDecimal("100"), zeroCost);
        p.executeSell(today, "AAPL", new BigDecimal("4"), new BigDecimal("110"), zeroCost);

        assertThat(p.cash().doubleValue()).isCloseTo(9440.0, within(0.0001));
        assertThat(p.holdings().get("AAPL")).isEqualByComparingTo("6");
    }

    @Test
    void value_marksToMarketAtSuppliedPrices() {
        var p = new Portfolio(new BigDecimal("5000"));
        p.executeBuy(today, "X", new BigDecimal("10"), new BigDecimal("100"), zeroCost);
        // 4000 cash + 10 shares * 120 = 4000 + 1200 = 5200
        assertThat(p.value(Map.of("X", new BigDecimal("120"))).doubleValue())
                .isCloseTo(5200.0, within(0.0001));
    }

    @Test
    void costsAreDeductedFromCash() {
        // 10 bps commission, 0 spread on a $1000 trade = $1 cost
        var withCost = new BpsCostModel(new BigDecimal("10"), BigDecimal.ZERO);
        var p = new Portfolio(new BigDecimal("10000"));
        p.executeBuy(today, "X", new BigDecimal("10"), new BigDecimal("100"), withCost);
        // Cash = 10000 - 1000 (notional) - 1 (cost) = 8999
        assertThat(p.cash().doubleValue()).isCloseTo(8999.0, within(0.0001));
    }
}
