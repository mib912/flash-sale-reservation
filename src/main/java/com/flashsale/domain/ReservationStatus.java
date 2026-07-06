package com.flashsale.domain;

public enum ReservationStatus {
    CONFIRMED("예약 확정"),
    FAILED("예약 실패");
    // Step 4(Kafka 비동기 확정)에서 PENDING 추가 예정

    private final String desc;

    ReservationStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
