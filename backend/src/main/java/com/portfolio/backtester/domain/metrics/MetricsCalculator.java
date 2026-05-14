package com.portfolio.backtester.domain.metrics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure function: equity curve in -> performance metrics out.
 *
 * Conventions:
 *  - Returns are simple period-over-period: (V_t - V_{t-1}) / V_{t-1}.
 *    Log returns would compose more cleanly but simple returns match what
 *    every Bloomberg / Excel calculation does, which is what reviewers
 *    will sanity-check against.
 *  - Annualization factor = 252 trading days/year.
 *  - VaR and CVaR are computed using the historical method on simple
 *    daily returns; reported as positive numbers (loss magnitudes).
 */
public final class MetricsCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int DECIMAL_SCALE = 8;
    private static final BigDecimal TRADING_DAYS = new BigDecimal("252");
    private static final BigDecimal SQRT_TRADING_DAYS = sqrt(TRADING_DAYS);

    private MetricsCalculator() {}

    public static PerformanceMetrics compute(
            List<BigDecimal> equityCurve,
            List<BigDecimal> benchmarkCurve,
            BigDecimal annualRiskFreeRate) {

        if (equityCurve == null || equityCurve.size() < 2) {
            throw new IllegalArgumentException("equity curve needs >= 2 points");
        }

        BigDecimal start = equityCurve.get(0);
        BigDecimal end = equityCurve.get(equityCurve.size() - 1);
        BigDecimal totalReturn = end.divide(start, MC).subtract(BigDecimal.ONE);

        int days = equityCurve.size() - 1;
        double yearsD = days / 252.0;
        BigDecimal cagr = BigDecimal.ZERO;
        if (yearsD > 0 && totalReturn.signum() > -1) {
            double base = end.doubleValue() / start.doubleValue();
            cagr = BigDecimal.valueOf(Math.pow(base, 1.0 / yearsD) - 1.0)
                    .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        List<BigDecimal> returns = simpleReturns(equityCurve);
        BigDecimal meanReturn = mean(returns);
        BigDecimal stdev = stdev(returns, meanReturn);
        BigDecimal volatility = stdev.multiply(SQRT_TRADING_DAYS, MC)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal dailyRf = annualRiskFreeRate.divide(TRADING_DAYS, MC);
        BigDecimal excessMean = meanReturn.subtract(dailyRf);
        BigDecimal sharpe = stdev.signum() == 0
                ? BigDecimal.ZERO
                : excessMean.divide(stdev, MC)
                    .multiply(SQRT_TRADING_DAYS, MC)
                    .setScale(4, RoundingMode.HALF_UP);

        BigDecimal downsideStdev = downsideDeviation(returns, dailyRf);
        BigDecimal sortino = downsideStdev.signum() == 0
                ? BigDecimal.ZERO
                : excessMean.divide(downsideStdev, MC)
                    .multiply(SQRT_TRADING_DAYS, MC)
                    .setScale(4, RoundingMode.HALF_UP);

        DrawdownResult dd = maxDrawdown(equityCurve);
        BigDecimal calmar = dd.maxDrawdown().signum() == 0
                ? BigDecimal.ZERO
                : cagr.divide(dd.maxDrawdown().abs(), 4, RoundingMode.HALF_UP);

        BigDecimal var95 = historicalVaR(returns, 0.95);
        BigDecimal cvar95 = historicalCVaR(returns, 0.95);

        BigDecimal beta = BigDecimal.ZERO;
        BigDecimal informationRatio = BigDecimal.ZERO;
        if (benchmarkCurve != null && benchmarkCurve.size() == equityCurve.size()) {
            List<BigDecimal> benchReturns = simpleReturns(benchmarkCurve);
            beta = beta(returns, benchReturns);
            informationRatio = informationRatio(returns, benchReturns);
        }

        return new PerformanceMetrics(
                totalReturn.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP),
                cagr,
                sharpe,
                sortino,
                calmar,
                dd.maxDrawdown(),
                dd.durationDays(),
                volatility,
                var95,
                cvar95,
                beta,
                informationRatio);
    }

    public static List<BigDecimal> simpleReturns(List<BigDecimal> series) {
        List<BigDecimal> out = new ArrayList<>(series.size() - 1);
        for (int i = 1; i < series.size(); i++) {
            BigDecimal prev = series.get(i - 1);
            BigDecimal curr = series.get(i);
            if (prev.signum() == 0) {
                out.add(BigDecimal.ZERO);
            } else {
                out.add(curr.divide(prev, MC).subtract(BigDecimal.ONE));
            }
        }
        return out;
    }

    static BigDecimal mean(List<BigDecimal> xs) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal x : xs) sum = sum.add(x);
        return sum.divide(BigDecimal.valueOf(xs.size()), MC);
    }

    /** Sample standard deviation (n-1 denominator). */
    static BigDecimal stdev(List<BigDecimal> xs, BigDecimal mean) {
        if (xs.size() < 2) return BigDecimal.ZERO;
        BigDecimal sumSq = BigDecimal.ZERO;
        for (BigDecimal x : xs) {
            BigDecimal diff = x.subtract(mean);
            sumSq = sumSq.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(xs.size() - 1L), MC);
        return sqrt(variance);
    }

    static BigDecimal downsideDeviation(List<BigDecimal> returns, BigDecimal target) {
        BigDecimal sumSq = BigDecimal.ZERO;
        int n = 0;
        for (BigDecimal r : returns) {
            BigDecimal diff = r.subtract(target);
            if (diff.signum() < 0) {
                sumSq = sumSq.add(diff.multiply(diff));
            }
            n++;
        }
        if (n == 0) return BigDecimal.ZERO;
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(n), MC);
        return sqrt(variance);
    }

    record DrawdownResult(BigDecimal maxDrawdown, int durationDays) {}

    static DrawdownResult maxDrawdown(List<BigDecimal> equity) {
        BigDecimal peak = equity.get(0);
        BigDecimal maxDD = BigDecimal.ZERO;
        int peakIdx = 0;
        int worstStart = 0;
        int worstEnd = 0;
        for (int i = 0; i < equity.size(); i++) {
            BigDecimal v = equity.get(i);
            if (v.compareTo(peak) > 0) {
                peak = v;
                peakIdx = i;
            }
            BigDecimal dd = v.subtract(peak).divide(peak, MC);
            if (dd.compareTo(maxDD) < 0) {
                maxDD = dd;
                worstStart = peakIdx;
                worstEnd = i;
            }
        }
        return new DrawdownResult(
                maxDD.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP),
                worstEnd - worstStart);
    }

    static BigDecimal historicalVaR(List<BigDecimal> returns, double confidence) {
        if (returns.isEmpty()) return BigDecimal.ZERO;
        double[] arr = returns.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        Arrays.sort(arr);
        int idx = (int) Math.floor((1.0 - confidence) * arr.length);
        idx = Math.max(0, Math.min(arr.length - 1, idx));
        return BigDecimal.valueOf(-arr[idx]).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal historicalCVaR(List<BigDecimal> returns, double confidence) {
        if (returns.isEmpty()) return BigDecimal.ZERO;
        double[] arr = returns.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        Arrays.sort(arr);
        int cutoff = (int) Math.floor((1.0 - confidence) * arr.length);
        cutoff = Math.max(1, cutoff);
        double sum = 0;
        for (int i = 0; i < cutoff; i++) sum += arr[i];
        return BigDecimal.valueOf(-sum / cutoff).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal beta(List<BigDecimal> port, List<BigDecimal> bench) {
        BigDecimal mp = mean(port);
        BigDecimal mb = mean(bench);
        BigDecimal covSum = BigDecimal.ZERO;
        BigDecimal varSum = BigDecimal.ZERO;
        for (int i = 0; i < port.size(); i++) {
            BigDecimal dp = port.get(i).subtract(mp);
            BigDecimal db = bench.get(i).subtract(mb);
            covSum = covSum.add(dp.multiply(db));
            varSum = varSum.add(db.multiply(db));
        }
        if (varSum.signum() == 0) return BigDecimal.ZERO;
        return covSum.divide(varSum, 4, RoundingMode.HALF_UP);
    }

    static BigDecimal informationRatio(List<BigDecimal> port, List<BigDecimal> bench) {
        List<BigDecimal> excess = new ArrayList<>(port.size());
        for (int i = 0; i < port.size(); i++) {
            excess.add(port.get(i).subtract(bench.get(i)));
        }
        BigDecimal m = mean(excess);
        BigDecimal s = stdev(excess, m);
        if (s.signum() == 0) return BigDecimal.ZERO;
        return m.divide(s, MC).multiply(SQRT_TRADING_DAYS, MC)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** Newton-Raphson square root for BigDecimal — accurate to 20 digits. */
    static BigDecimal sqrt(BigDecimal x) {
        if (x.signum() < 0) {
            throw new ArithmeticException("sqrt of negative: " + x);
        }
        if (x.signum() == 0) return BigDecimal.ZERO;
        BigDecimal two = BigDecimal.valueOf(2);
        BigDecimal guess = BigDecimal.valueOf(Math.sqrt(x.doubleValue()));
        for (int i = 0; i < 20; i++) {
            guess = guess.add(x.divide(guess, MC)).divide(two, MC);
        }
        return guess;
    }
}
