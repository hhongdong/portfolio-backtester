export type BacktestStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface Metrics {
  totalReturn: number | null;
  cagr: number | null;
  sharpe: number | null;
  sortino: number | null;
  calmar: number | null;
  maxDrawdown: number | null;
  maxDrawdownDurationDays: number | null;
  volatility: number | null;
  var95: number | null;
  cvar95: number | null;
  beta: number | null;
  informationRatio: number | null;
}

export interface BacktestResponse {
  id: string;
  name?: string;
  status: BacktestStatus;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  metrics?: Metrics;
}

export interface EquityPoint {
  date: string;
  portfolioValue: number;
  benchmarkValue: number | null;
}

export interface Trade {
  date: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  cost: number;
}

export interface SymbolDto {
  symbol: string;
  name: string;
  sector: string | null;
  listedAt: string | null;
  delistedAt: string | null;
}

export interface BacktestRequest {
  name: string;
  startDate: string;
  endDate: string;
  initialCapital: number;
  universe: string[];
  strategy: { type: string; rebalancePeriod: string };
  execution: {
    transactionCostBps: number;
    bidAskSpreadBps: number;
    slippageModel: string;
  };
  benchmark?: string;
  annualRiskFreeRate?: number;
}
