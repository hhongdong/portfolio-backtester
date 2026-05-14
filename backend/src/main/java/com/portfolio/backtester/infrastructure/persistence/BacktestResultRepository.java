package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BacktestResultRepository extends JpaRepository<BacktestResultEntity, UUID> {}
