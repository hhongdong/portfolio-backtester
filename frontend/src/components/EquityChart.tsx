import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { EquityPoint } from '../types';

interface Props {
  points: EquityPoint[];
}

export function EquityChart({ points }: Props) {
  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4">
      <h3 className="font-semibold mb-3">Equity Curve</h3>
      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={points} margin={{ left: 10, right: 20, top: 10, bottom: 10 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={40} />
          <YAxis
            tick={{ fontSize: 11 }}
            tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`}
          />
          <Tooltip
            formatter={(v: number) => `$${v.toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
          />
          <Legend />
          <Line
            type="monotone"
            dataKey="portfolioValue"
            name="Portfolio"
            stroke="#1e40af"
            dot={false}
            strokeWidth={2}
          />
          <Line
            type="monotone"
            dataKey="benchmarkValue"
            name="Benchmark"
            stroke="#64748b"
            dot={false}
            strokeWidth={1.5}
            strokeDasharray="4 2"
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
