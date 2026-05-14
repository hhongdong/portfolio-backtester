package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SymbolRepository extends JpaRepository<SymbolEntity, String> {
    @Query("SELECT s FROM SymbolEntity s WHERE s.delistedAt IS NULL OR s.delistedAt > :date")
    List<SymbolEntity> findActiveAt(@Param("date") LocalDate date);
}
