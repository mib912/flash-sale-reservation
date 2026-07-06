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
     * Step 0: 락 없는 naive 예약 로직 (feature/step0 브랜치에서 직접 구현).
     *
     * 흐름:
     *   1) productId로 재고 조회 (없으면 예외)
     *   2) 재고가 0 이하인지 확인 → 매진이면 실패 처리
     *   3) 재고가 있으면 decrease()
     *   4) CONFIRMED 예약 생성 후 저장
     *   5) 성공 여부 반환
     *
     * 락이 없어 동시 요청 시 lost update / oversell 이 발생한다.
     * 이 "터지는 상태"를 부하 테스트로 재현하는 것이 Step 0의 목표.
     *
     * 쓸 재료:
     *   - stockRepository.findByProductId(productId) -> Optional<Stock> (.orElseThrow(...))
     *   - stock.getQuantity(), stock.decrease()
     *   - new Reservation(userId, productId, ReservationStatus.CONFIRMED)
     *   - reservationRepository.save(...)
     */
    @Transactional
    public boolean reserve(Long userId, Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
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
