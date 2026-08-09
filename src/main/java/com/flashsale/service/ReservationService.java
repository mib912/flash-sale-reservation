package com.flashsale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationAttempter attempter;
    private final RetryMetrics retryMetrics;   // 재시도 계측용 카운터 (주입)

    private static final int MAX_ATTEMPTS = 100;

    /**
     * Step 2: 낙관락 재시도 처리 루프
     * 트랜잭션은 attempter.attempt()가 매 호출마다 새로 가짐
     * 예약 실패 시 재시도 하여 재고처리 진행
     */
    public boolean reserve(Long userId, Long productId) {
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                return attempter.attempt(userId, productId);
            } catch (OptimisticLockingFailureException e) {
                // 낙관락 충돌 -> 재고는 있지만 version이 달라 실패
                retryMetrics.incrementRetry();   // 재시도 발생 → 카운터 원자적 +1
                attempts++;
            }
        }
        return false; // 재시도 max 횟수 진행
    }
}
