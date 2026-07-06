package com.flashsale.repository;

import com.flashsale.domain.Reservation;
import com.flashsale.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByProductId(Long productId);

    // 통계용: 상태별 예약 건수 (CONFIRMED 건수 > 초기 재고 → oversell 재현 성공)
    long countByProductIdAndStatus(Long productId, ReservationStatus status);

    void deleteByProductId(Long productId);
}
