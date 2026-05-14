package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BacktestRunRepository extends JpaRepository<BacktestRunEntity, UUID> {
    Optional<BacktestRunEntity> findByIdempotencyKey(String idempotencyKey);
    Page<BacktestRunEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
