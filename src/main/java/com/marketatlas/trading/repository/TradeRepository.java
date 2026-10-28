package com.marketatlas.trading.repository;

import com.marketatlas.trading.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // Units held = buys minus sells. Returns null when the account has never traded the asset.
    @Query("""
            select sum(case when t.side = com.marketatlas.trading.model.TradeSide.BUY
                            then t.quantity else -t.quantity end)
            from Trade t
            where t.account.id = :accountId and t.asset.id = :assetId
            """)
    BigDecimal netQuantity(@Param("accountId") Long accountId, @Param("assetId") Long assetId);

    Optional<Trade> findByAccountIdAndIdempotencyKey(Long accountId, String idempotencyKey);

    List<Trade> findByAccountIdOrderByExecutedAtAscIdAsc(Long accountId);

    Page<Trade> findByAccountId(Long accountId, Pageable pageable);
}