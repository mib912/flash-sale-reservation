package com.flashsale.controller;

import com.flashsale.service.TestSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 지원용 API (프로덕션 API 아님).
 *
 *   POST /api/reset?productId=1&quantity=100  → 재고/예약 초기화
 *   GET  /api/stats?productId=1               → 남은 재고, CONFIRMED/FAILED 건수
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestSupportController {

    private final TestSupportService testSupportService;

    @PostMapping("/reset")
    public void reset(@RequestParam Long productId,
                      @RequestParam(defaultValue = "100") int quantity) {
        testSupportService.reset(productId, quantity);
    }

    @GetMapping("/stats")
    public TestSupportService.Stats stats(@RequestParam Long productId) {
        return testSupportService.stats(productId);
    }
}
