import { FormEvent, ReactNode, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { BacktestRequest } from '../types';

const inputCls =
  'w-full border border-slate-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

interface Props {
  onSubmitted: (id: string) => void;
}

const DEFAULT_PICKS = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'JPM', 'JNJ', 'XOM'];

export function ConfigureView({ onSubmitted }: Props) {
  const symbolsQuery = useQuery({ queryKey: ['symbols'], queryFn: api.listSymbols });

  const [name, setName] = useState('Equal-weight blue chips');
  const [startDate, setStartDate] = useState('2018-01-01');
  const [endDate, setEndDate] = useState('2023-12-31');
  const [initialCapital, setInitialCapital] = useState(100_000);
  const [universe, setUniverse] = useState<string[]>(DEFAULT_PICKS);
  const [rebalancePeriod, setRebalancePeriod] = useState('QUARTERLY');
  const [costBps, setCostBps] = useState(5);
  const [spreadBps, setSpreadBps] = useState(10);
  const [benchmark, setBenchmark] = useState('SPY');

  const submit = useMutation({
    mutationFn: (req: BacktestRequest) => api.submitBacktest(req),
    onSuccess: (res) => onSubmitted(res.id),
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    submit.mutate({
      name,
      startDate,
      endDate,
      initialCapital,
      universe,
      strategy: { type: 'EQUAL_WEIGHT', rebalancePeriod },
      execution: {
        transactionCostBps: costBps,
        bidAskSpreadBps: spreadBps,
        slippageModel: 'NONE',
      },
      benchmark: benchmark || undefined,
      annualRiskFreeRate: 0.02,
    });
  };

  const toggleSymbol = (sym: string) =>
    setUniverse((cur) =>
      cur.includes(sym) ? cur.filter((s) => s !== sym) : [...cur, sym],
    );

  return (
    <form onSubmit={onSubmit} className="bg-white rounded-lg border border-slate-200 p-6 space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Field label="Backtest name">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputCls}
          />
        </Field>
        <Field label="Initial capital ($)">
          <input
            type="number"
            min={1000}
            step={1000}
            value={initialCapital}
            onChange={(e) => setInitialCapital(Number(e.target.value))}
            className={inputCls}
          />
        </Field>
        <Field label="Start date">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className={inputCls}
          />
        </Field>
        <Field label="End date">
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className={inputCls}
          />
        </Field>
        <Field label="Rebalance period">
          <select
            value={rebalancePeriod}
            onChange={(e) => setRebalancePeriod(e.target.value)}
            className={inputCls}
          >
            {['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'].map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </Field>
        <Field label="Benchmark (optional)">
          <input
            type="text"
            value={benchmark}
            onChange={(e) => setBenchmark(e.target.value.toUpperCase())}
            className={inputCls}
            placeholder="SPY"
          />
        </Field>
        <Field label="Commission (bps)">
          <input
            type="number"
            min={0}
            value={costBps}
            onChange={(e) => setCostBps(Number(e.target.value))}
            className={inputCls}
          />
        </Field>
        <Field label="Bid-ask spread (bps)">
          <input
            type="number"
            min={0}
            value={spreadBps}
            onChange={(e) => setSpreadBps(Number(e.target.value))}
            className={inputCls}
          />
        </Field>
      </div>

      <div>
        <label className="text-sm font-medium text-slate-700">Universe</label>
        <p className="text-xs text-slate-500 mb-2">
          Click symbols to include. Selected: {universe.length}
        </p>
        <div className="flex flex-wrap gap-2">
          {symbolsQuery.data?.map((s) => {
            const selected = universe.includes(s.symbol);
            return (
              <button
                key={s.symbol}
                type="button"
                onClick={() => toggleSymbol(s.symbol)}
                className={`px-2.5 py-1 rounded border text-xs transition ${
                  selected
                    ? 'bg-blue-600 border-blue-600 text-white'
                    : 'bg-white border-slate-300 text-slate-700 hover:border-slate-400'
                }`}
              >
                {s.symbol}
              </button>
            );
          }) ?? <span className="text-sm text-slate-500">Loading symbols…</span>}
        </div>
      </div>

      <div className="flex items-center gap-3 pt-2">
        <button
          type="submit"
          disabled={submit.isPending || universe.length === 0}
          className="bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 text-white px-5 py-2 rounded font-medium"
        >
          {submit.isPending ? 'Submitting…' : 'Run backtest'}
        </button>
        {submit.isError && (
          <span className="text-sm text-red-600">{(submit.error as Error).message}</span>
        )}
      </div>

    </form>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
