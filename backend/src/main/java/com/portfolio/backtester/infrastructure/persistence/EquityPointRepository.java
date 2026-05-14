package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EquityPointRepository extends JpaRepository<EquityPointEntity, EquityPointEntity.Key> {

    @Query("""
            SELECT e FROM EquityPointEntity e
            WHERE e.id.backtestId = :id
            ORDER BY e.id.tradeDate
            """)
    List<EquityPointEntity> findByBacktestId(@Param("id") UUID backtestId);
}
