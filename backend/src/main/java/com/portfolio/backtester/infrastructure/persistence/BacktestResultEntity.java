package com.portfolio.backtester.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "backtest_results")
public class BacktestResultEntity {

    @Id
    @Column(name = "backtest_id")
    private UUID backtestId;

    @Column(name = "total_return") private BigDecimal totalReturn;
    @Column private BigDecimal cagr;
    @Column private BigDecimal sharpe;
    @Column private BigDecimal sortino;
    @Column private BigDecimal calmar;
    @Column(name = "max_drawdown") private BigDecimal maxDrawdown;
    @Column(name = "max_drawdown_duration_days") private Integer maxDrawdownDurationDays;
    @Column private BigDecimal volatility;
    @Column(name = "var_95") private BigDecimal var95;
    @Column(name = "cvar_95") private BigDecimal cvar95;
    @Column private BigDecimal beta;
    @Column(name = "information_ratio") private BigDecimal informationRatio;

    @Type(JsonBinaryType.class)
    @Column(name = "metrics_json", columnDefinition = "jsonb")
    private String metricsJson;

    public UUID getBacktestId() { return backtestId; }
    public void setBacktestId(UUID backtestId) { this.backtestId = backtestId; }
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal v) { this.totalReturn = v; }
    public BigDecimal getCagr() { return cagr; }
    public void setCagr(BigDecimal v) { this.cagr = v; }
    public BigDecimal getSharpe() { return sharpe; }
    public void setSharpe(BigDecimal v) { this.sharpe = v; }
    public BigDecimal getSortino() { return sortino; }
    public void setSortino(BigDecimal v) { this.sortino = v; }
    public BigDecimal getCalmar() { return calmar; }
    public void setCalmar(BigDecimal v) { this.calmar = v; }
    public BigDecimal getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(BigDecimal v) { this.maxDrawdown = v; }
    public Integer getMaxDrawdownDurationDays() { return maxDrawdownDurationDays; }
    public void setMaxDrawdownDurationDays(Integer v) { this.maxDrawdownDurationDays = v; }
    public BigDecimal getVolatility() { return volatility; }
    public void setVolatility(BigDecimal v) { this.volatility = v; }
    public BigDecimal getVar95() { return var95; }
    public void setVar95(BigDecimal v) { this.var95 = v; }
    public BigDecimal getCvar95() { return cvar95; }
    public void setCvar95(BigDecimal v) { this.cvar95 = v; }
    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal v) { this.beta = v; }
    public BigDecimal getInformationRatio() { return informationRatio; }
    public void setInformationRatio(BigDecimal v) { this.informationRatio = v; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String v) { this.metricsJson = v; }
}
