import {
  BacktestRequest,
  BacktestResponse,
  EquityPoint,
  SymbolDto,
  Trade,
} from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const BASE = `${API_BASE_URL}/api/v1`;

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status}: ${text}`);
  }
  return res.json();
}

export const api = {
  listSymbols: () => http<SymbolDto[]>("/symbols"),
  submitBacktest: (req: BacktestRequest) =>
    http<BacktestResponse>("/backtests", {
      method: "POST",
      body: JSON.stringify(req),
    }),
  getBacktest: (id: string) => http<BacktestResponse>(`/backtests/${id}`),
  getEquity: (id: string) => http<EquityPoint[]>(`/backtests/${id}/equity`),
  getTrades: (id: string) =>
    http<{ content: Trade[] }>(`/backtests/${id}/trades?size=500`),
  listBacktests: () =>
    http<{ content: BacktestResponse[] }>("/backtests?size=20"),
};
