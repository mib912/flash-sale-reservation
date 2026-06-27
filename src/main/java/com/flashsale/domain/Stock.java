package com.flashsale.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고. 동시성 제어의 핵심 대상.
 *
 * Step 0(현재): 어떤 락도 없다. decrease()는 단순히 수량을 1 줄인다.
 * 여러 트랜잭션이 동시에 같은 재고를 읽고 차감하면 lost update / oversell 이 발생한다.
 * 이 "터지는 상태"를 부하 테스트로 재현하는 것이 Step 0의 목표.
 *
 * Step 2에서 @Version(낙관락), Step 1에서 비관락(@Lock)을 적용하며 비교한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int quantity;

    public Stock(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * Step 0: 가드 없는 순진한 차감.
     * 재고 부족 판단은 서비스 계층에서 하고, 여기서는 그냥 1 감소시킨다.
     * 락이 없으므로 동시 요청 시 음수까지 내려갈 수 있다(= oversell).
     */
    public void decrease() {
        this.quantity--;
    }

    public void reset(int quantity) {
        this.quantity = quantity;
    }
}
