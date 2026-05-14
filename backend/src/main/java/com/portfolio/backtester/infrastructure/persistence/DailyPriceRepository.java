package com.portfolio.backtester.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPriceEntity, DailyPriceEntity.Key> {

    @Query("""
            SELECT p FROM DailyPriceEntity p
            WHERE p.id.symbol IN :symbols
              AND p.id.tradeDate BETWEEN :from AND :to
            ORDER BY p.id.symbol, p.id.tradeDate
            """)
    List<DailyPriceEntity> findRange(@Param("symbols") List<String> symbols,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("""
            SELECT p FROM DailyPriceEntity p
            WHERE p.id.symbol = :symbol
              AND p.id.tradeDate BETWEEN :from AND :to
            ORDER BY p.id.tradeDate
            """)
    List<DailyPriceEntity> findForSymbol(@Param("symbol") String symbol,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    @Query("SELECT MAX(p.id.tradeDate) FROM DailyPriceEntity p WHERE p.id.symbol = :symbol")
    Optional<LocalDate> latestDateFor(@Param("symbol") String symbol);
}
