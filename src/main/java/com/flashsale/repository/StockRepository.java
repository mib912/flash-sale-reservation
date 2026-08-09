package com.flashsale.repository;

import com.flashsale.domain.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // Step 0: 일반 조회 (락 없음)
    Optional<Stock> findByProductId(Long productId);
}
