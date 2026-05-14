package com.portfolio.backtester.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "symbols")
public class SymbolEntity {

    @Id
    private String symbol;

    private String name;
    private String sector;

    @Column(name = "listed_at")
    private LocalDate listedAt;

    @Column(name = "delisted_at")
    private LocalDate delistedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public LocalDate getListedAt() { return listedAt; }
    public void setListedAt(LocalDate listedAt) { this.listedAt = listedAt; }
    public LocalDate getDelistedAt() { return delistedAt; }
    public void setDelistedAt(LocalDate delistedAt) { this.delistedAt = delistedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
