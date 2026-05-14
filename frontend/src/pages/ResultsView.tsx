import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { MetricsGrid } from '../components/MetricsGrid';
import { EquityChart } from '../components/EquityChart';
import { DrawdownChart } from '../components/DrawdownChart';

interface Props {
  backtestId: string;
  onReset: () => void;
}

export function ResultsView({ backtestId, onReset }: Props) {
  const runQuery = useQuery({
    queryKey: ['backtest', backtestId],
    queryFn: () => api.getBacktest(backtestId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      // Stop polling once the run reaches a terminal state
      return status === 'SUCCESS' || status === 'FAILED' ? false : 1500;
    },
  });

  const isDone = runQuery.data?.status === 'SUCCESS';

  const equityQuery = useQuery({
    queryKey: ['equity', backtestId],
    queryFn: () => api.getEquity(backtestId),
    enabled: isDone,
  });

  const tradesQuery = useQuery({
    queryKey: ['trades', backtestId],
    queryFn: () => api.getTrades(backtestId),
    enabled: isDone,
  });

  const status = runQuery.data?.status ?? 'PENDING';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <button onClick={onReset} className="text-sm text-blue-600 hover:underline">
            ← New backtest
          </button>
          <h2 className="text-2xl font-semibold mt-1">
            {runQuery.data?.name || 'Backtest'}
          </h2>
          <p className="text-sm text-slate-500 font-mono">{backtestId}</p>
        </div>
        <StatusBadge status={status} />
      </div>

      {status === 'FAILED' && (
        <div className="bg-red-50 border border-red-200 rounded p-4 text-sm text-red-800">
          <strong>Backtest failed:</strong> {runQuery.data?.errorMessage ?? 'unknown error'}
        </div>
      )}

      {!isDone && status !== 'FAILED' && (
        <div className="bg-blue-50 border border-blue-200 rounded p-6 text-center">
          <div className="animate-pulse text-blue-700 font-medium">
            Running backtest…
          </div>
          <div className="text-xs text-blue-600 mt-1">
            polling /api/v1/backtests/{backtestId.slice(0, 8)}…
          </div>
        </div>
      )}

      {isDone && runQuery.data?.metrics && (
        <>
          <MetricsGrid metrics={runQuery.data.metrics} />
          {equityQuery.data && equityQuery.data.length > 0 && (
            <>
              <EquityChart points={equityQuery.data} />
              <DrawdownChart points={equityQuery.data} />
            </>
          )}
          {tradesQuery.data && (
            <div className="bg-white rounded-lg border border-slate-200 p-4">
              <h3 className="font-semibold mb-3">
                Trades ({tradesQuery.data.content.length})
              </h3>
              <div className="overflow-auto max-h-96">
                <table className="min-w-full text-sm">
                  <thead className="text-xs uppercase text-slate-500 border-b">
                    <tr>
                      <th className="text-left py-2 pr-3">Date</th>
                      <th className="text-left pr-3">Symbol</th>
                      <th className="text-left pr-3">Side</th>
                      <th className="text-right pr-3">Qty</th>
                      <th className="text-right pr-3">Price</th>
                      <th className="text-right">Cost</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tradesQuery.data.content.slice(0, 200).map((t, i) => (
                      <tr key={i} className="border-b border-slate-100">
                        <td className="py-1.5 pr-3 font-mono text-xs">{t.date}</td>
                        <td className="pr-3">{t.symbol}</td>
                        <td className={`pr-3 font-medium ${t.side === 'BUY' ? 'text-green-700' : 'text-red-700'}`}>
                          {t.side}
                        </td>
                        <td className="text-right pr-3">{t.quantity.toFixed(4)}</td>
                        <td className="text-right pr-3">${t.price.toFixed(2)}</td>
                        <td className="text-right">${t.cost.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const cls = {
    PENDING: 'bg-slate-100 text-slate-700',
    RUNNING: 'bg-blue-100 text-blue-700 animate-pulse',
    SUCCESS: 'bg-green-100 text-green-700',
    FAILED: 'bg-red-100 text-red-700',
  }[status] ?? 'bg-slate-100 text-slate-700';
  return (
    <span className={`px-3 py-1 rounded-full text-xs font-medium ${cls}`}>
      {status}
    </span>
  );
}
