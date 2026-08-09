package com.flashsale.repository;

import com.flashsale.domain.Stock;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // Step 0: 일반 조회 (락 없음)
    Optional<Stock> findByProductId(Long productId);

    // Step 1: 비관락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productId = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") Long productId);
}
