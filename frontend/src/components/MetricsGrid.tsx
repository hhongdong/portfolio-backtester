import { Metrics } from '../types';

interface Props {
  metrics: Metrics;
}

const pct = (n: number | null | undefined, digits = 2) =>
  n == null ? '—' : `${(n * 100).toFixed(digits)}%`;

const num = (n: number | null | undefined, digits = 2) =>
  n == null ? '—' : n.toFixed(digits);

export function MetricsGrid({ metrics }: Props) {
  const cards: Array<{ label: string; value: string; hint?: string }> = [
    { label: 'Total Return', value: pct(metrics.totalReturn) },
    { label: 'CAGR', value: pct(metrics.cagr), hint: 'annualized' },
    { label: 'Sharpe', value: num(metrics.sharpe), hint: 'higher = better risk-adj return' },
    { label: 'Sortino', value: num(metrics.sortino), hint: 'downside-only Sharpe' },
    { label: 'Calmar', value: num(metrics.calmar), hint: 'CAGR / |max drawdown|' },
    { label: 'Max Drawdown', value: pct(metrics.maxDrawdown) },
    { label: 'DD Duration', value: `${metrics.maxDrawdownDurationDays ?? '—'}d` },
    { label: 'Volatility', value: pct(metrics.volatility) },
    { label: 'VaR (95%)', value: pct(metrics.var95), hint: 'historical, daily' },
    { label: 'CVaR (95%)', value: pct(metrics.cvar95), hint: 'expected loss in worst 5%' },
    { label: 'Beta', value: num(metrics.beta) },
    { label: 'Information Ratio', value: num(metrics.informationRatio) },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
      {cards.map((c) => (
        <div key={c.label} className="bg-white rounded-lg border border-slate-200 p-4">
          <div className="text-xs uppercase tracking-wide text-slate-500">{c.label}</div>
          <div className="text-2xl font-semibold mt-1">{c.value}</div>
          {c.hint && <div className="text-xs text-slate-400 mt-1">{c.hint}</div>}
        </div>
      ))}
    </div>
  );
}
