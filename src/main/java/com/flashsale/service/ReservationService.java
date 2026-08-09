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
public class ReservationService {

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Step 1: 비관락으로 재고조회 시점 부터 락 설정
     */
    @Transactional
    public boolean reserve(Long userId, Long productId) {
        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("stock not found: productId=" + productId));

        if (stock.getQuantity() <= 0) {
            return false;
        } else {
            stock.decrease();
        }
        reservationRepository.save(new Reservation(userId, productId, ReservationStatus.CONFIRMED));

        return true;
    }
}
