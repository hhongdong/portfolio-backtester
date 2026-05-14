import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useMemo } from 'react';
import { EquityPoint } from '../types';

interface Props {
  points: EquityPoint[];
}

export function DrawdownChart({ points }: Props) {
  const series = useMemo(() => {
    let peak = 0;
    return points.map((p) => {
      peak = Math.max(peak, p.portfolioValue);
      const drawdown = peak === 0 ? 0 : (p.portfolioValue - peak) / peak;
      return { date: p.date, drawdown: drawdown * 100 };
    });
  }, [points]);

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4">
      <h3 className="font-semibold mb-3">Drawdown</h3>
      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={series} margin={{ left: 10, right: 20, top: 10, bottom: 10 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={40} />
          <YAxis
            tick={{ fontSize: 11 }}
            tickFormatter={(v) => `${v.toFixed(0)}%`}
          />
          <Tooltip formatter={(v: number) => `${v.toFixed(2)}%`} />
          <Area
            type="monotone"
            dataKey="drawdown"
            stroke="#dc2626"
            fill="#fecaca"
            fillOpacity={0.6}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
