package com.flashsale.service;

import com.flashsale.domain.ReservationStatus;
import com.flashsale.domain.Stock;
import com.flashsale.repository.ReservationRepository;
import com.flashsale.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부하 테스트 지원용 서비스 (프로덕션 로직 아님).
 * 반복 측정 전 상태 초기화(reset)와 결과 집계(stats)를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class TestSupportService {

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 재고를 지정 수량으로 되돌리고 해당 상품의 예약을 전부 지운다.
     * 부하 테스트 반복 전마다 호출 (k6 setup()에서 자동 호출됨).
     */
    @Transactional
    public void reset(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("stock not found: productId=" + productId));
        stock.reset(quantity);
        reservationRepository.deleteByProductId(productId);
    }

    /**
     * 측정 결과 집계.
     * confirmedCount가 초기 재고(100)를 넘거나 remainingQuantity가 음수면 oversell 재현 성공.
     */
    @Transactional(readOnly = true)
    public Stats stats(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("stock not found: productId=" + productId));
        long confirmed = reservationRepository.countByProductIdAndStatus(productId, ReservationStatus.CONFIRMED);
        long failed = reservationRepository.countByProductIdAndStatus(productId, ReservationStatus.FAILED);
        return new Stats(productId, stock.getQuantity(), confirmed, failed);
    }

    public record Stats(Long productId, int remainingQuantity, long confirmedCount, long failedCount) {
    }
}
