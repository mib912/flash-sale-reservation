package com.flashsale.service;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 낙관락 재시도 횟수를 세는 계측용 카운터 (프로덕션 로직 아님, 측정 전용).
 *
 * 왜 AtomicLong인가:
 *   재시도는 수많은 스레드에서 동시에 일어난다. 일반 long의 count++는
 *   읽기-증가-쓰기 3단계라 원자적이지 않아, 동시 증가 시 서로 덮어써(lost update)
 *   정확히 세지 못한다. AtomicLong.incrementAndGet()은 이 증가를 원자적으로 처리해
 *   락 없이도 스레드 안전하게 카운트한다.
 *
 * 여러 빈(ReservationService가 증가, TestSupportService가 조회/초기화)이
 * 같은 카운터를 공유해야 하므로 별도 스프링 빈으로 분리한다.
 */
@Component
public class RetryMetrics {

    private final AtomicLong retryCount = new AtomicLong(0);

    /** 재시도 1회 발생 시 호출. 원자적으로 +1. */
    public void incrementRetry() {
        retryCount.incrementAndGet();
    }

    /** 현재까지 누적된 재시도 총 횟수. */
    public long getRetryCount() {
        return retryCount.get();
    }

    /** 부하 테스트 반복 전 0으로 초기화 (reset에서 호출). */
    public void reset() {
        retryCount.set(0);
    }
}
