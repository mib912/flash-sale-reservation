package com.flashsale.service;

import com.flashsale.domain.Reservation;
import com.flashsale.domain.ReservationStatus;
import com.flashsale.domain.Stock;
import com.flashsale.repository.ReservationRepository;
import com.flashsale.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationAttempter {
    private final ReservationRepository reservationRepository;
    private final StockRepository stockRepository;

    @Transactional
    public boolean attempt(Long userId, Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        if (stock.getQuantity() <= 0) {
            return false; // 매진
        } else {
            stock.decrease();
            reservationRepository.save(new Reservation(userId, productId, ReservationStatus.CONFIRMED));

            return true;
        }
    }

}
