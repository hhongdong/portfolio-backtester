package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
    Page<TradeEntity> findByBacktestIdOrderByTradeDateAsc(UUID backtestId, Pageable pageable);
}
