package com.flashsale.repository;

import com.flashsale.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByProductId(Long productId);

    void deleteByProductId(Long productId);
}
