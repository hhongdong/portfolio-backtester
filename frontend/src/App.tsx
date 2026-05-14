import { useState } from 'react';
import { ConfigureView } from './pages/ConfigureView';
import { ResultsView } from './pages/ResultsView';

export default function App() {
  const [activeBacktestId, setActiveBacktestId] = useState<string | null>(null);

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">Portfolio Backtester</h1>
        <p className="text-slate-600 mt-1">
          Walk-forward backtests with realistic execution. Look-ahead bias prevented by construction.
        </p>
      </header>

      {activeBacktestId ? (
        <ResultsView
          backtestId={activeBacktestId}
          onReset={() => setActiveBacktestId(null)}
        />
      ) : (
        <ConfigureView onSubmitted={(id) => setActiveBacktestId(id)} />
      )}
    </div>
  );
}
