package com.flashsale.repository;

import com.flashsale.domain.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // Step 0: 일반 조회 (락 없음)
    Optional<Stock> findByProductId(Long productId);

    // Step 1에서 비관락 메서드를 여기에 추가한다:
    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("select s from Stock s where s.productId = :productId")
    // Optional<Stock> findByProductIdForUpdate(Long productId);
}
