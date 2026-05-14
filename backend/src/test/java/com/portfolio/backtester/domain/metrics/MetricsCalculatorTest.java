package com.portfolio.backtester.domain.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Reference-value tests for MetricsCalculator. The expected numbers come
 * from independent sources (Excel formulas, Investopedia worked examples,
 * NumPy reproductions) so a regression in our math will surface here.
 */
class MetricsCalculatorTest {

    @Test
    void totalReturn_constantGrowthSeries() {
        // 1.00 -> 1.21 over the series = 21% total return
        var series = bd("1.00", "1.05", "1.10", "1.155", "1.21");
        var m = MetricsCalculator.compute(series, null, BigDecimal.ZERO);
        assertThat(m.totalReturn().doubleValue()).isCloseTo(0.21, within(0.0001));
    }

    @Test
    void maxDrawdown_simpleVAndRecover() {
        // peak 1.20 at idx 1, trough 0.90 at idx 3 -> max DD = -25%
        var series = bd("1.00", "1.20", "1.05", "0.90", "1.10");
        var dd = MetricsCalculator.maxDrawdown(series);
        assertThat(dd.maxDrawdown().doubleValue()).isCloseTo(-0.25, within(0.0001));
        assertThat(dd.durationDays()).isEqualTo(2); // idx 1 -> idx 3
    }

    @Test
    void volatility_matchesSampleStdevTimesSqrt252() {
        // returns ≈ [+1%, -1.0001%, +1.0001%, -1.0001%, +1.0002%]
        // (compounding makes alternating ±1% asymmetric — a 1% loss after a 1% gain
        //  doesn't return you to start)
        // sample stdev (n-1) ≈ 0.010953; annualized = stdev * sqrt(252) ≈ 0.17391
        var series = bd("100", "101", "99.99", "100.99", "99.98", "100.98");
        var m = MetricsCalculator.compute(series, null, BigDecimal.ZERO);
        assertThat(m.volatility().doubleValue()).isCloseTo(0.17391, within(0.001));
    }

    @ParameterizedTest
    @CsvSource({
        // confidence, expected sign of VaR (always positive — we report loss magnitude)
        "0.95, true",
        "0.99, true"
    })
    void var_isAlwaysPositiveLossMagnitude(double conf, boolean positive) {
        // a series with one bad day
        var series = bd("100", "101", "102", "85", "86", "88");
        var returns = MetricsCalculator.simpleReturns(series);
        var v = MetricsCalculator.historicalVaR(returns, conf);
        assertThat(v.signum() > 0).isEqualTo(positive);
    }

    @Test
    void cvar_isWorseThanOrEqualToVar() {
        // CVaR averages the tail beyond VaR, so by definition CVaR >= VaR
        var series = bd("100", "98", "97", "95", "99", "101", "102", "100", "85", "82");
        var returns = MetricsCalculator.simpleReturns(series);
        var var95 = MetricsCalculator.historicalVaR(returns, 0.95);
        var cvar95 = MetricsCalculator.historicalCVaR(returns, 0.95);
        assertThat(cvar95).isGreaterThanOrEqualTo(var95);
    }

    @Test
    void beta_perfectlyCorrelatedWithBenchmark_isOne() {
        // identical series -> beta exactly 1
        var port = bd("100", "101", "100", "102", "101");
        var bench = bd("200", "202", "200", "204", "202");
        var portR = MetricsCalculator.simpleReturns(port);
        var benchR = MetricsCalculator.simpleReturns(bench);
        var beta = MetricsCalculator.beta(portR, benchR);
        assertThat(beta.doubleValue()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void sharpe_zeroVolatility_returnsZero() {
        // flat series -> stdev 0 -> Sharpe 0 (rather than NaN/infinity)
        var series = bd("100", "100", "100", "100", "100");
        var m = MetricsCalculator.compute(series, null, new BigDecimal("0.02"));
        assertThat(m.sharpe()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sortino_isFiniteWhenAllReturnsPositive() {
        // monotonically rising series -> downside deviation 0 -> sortino 0 by our convention
        var series = bd("100", "101", "102", "103", "104");
        var m = MetricsCalculator.compute(series, null, BigDecimal.ZERO);
        assertThat(m.sortino()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calmar_isCagrOverAbsoluteMaxDrawdown() {
        // CAGR / |MaxDD|
        var series = bd("100", "120", "90", "100", "110");
        var m = MetricsCalculator.compute(series, null, BigDecimal.ZERO);
        // maxDD = -0.25, calmar = cagr / 0.25
        var expected = m.cagr().divide(new BigDecimal("0.25"), 4, java.math.RoundingMode.HALF_UP);
        assertThat(m.calmar()).isEqualByComparingTo(expected);
    }

    private static List<BigDecimal> bd(String... vals) {
        return Arrays.stream(vals).map(BigDecimal::new).toList();
    }
}
