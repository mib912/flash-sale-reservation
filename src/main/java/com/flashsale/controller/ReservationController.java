package com.flashsale.controller;

import com.flashsale.service.ReservationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 API. 부하 테스트(k6)가 이 엔드포인트를 때린다.
 *
 * 응답 규약 (k6 스크립트와 맞춰져 있음 — 바꾸면 load-test/step0.js도 같이 바꿀 것):
 *   200 OK       → 예약 성공 (CONFIRMED)
 *   409 CONFLICT → 매진으로 실패 (FAILED)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    public record ReserveRequest(@NotNull Long userId, @NotNull Long productId) {
    }

    @PostMapping("/reservations")
    public ResponseEntity<Void> reserve(@RequestBody ReserveRequest request) {
        boolean success = reservationService.reserve(request.userId(), request.productId());
        return success
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
