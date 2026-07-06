// Step 0: 락 없는 baseline — oversell 재현용 부하 테스트
//
// 실행: k6 run load-test/step0.js
// 시나리오: 1,000명이 동시에 각 1건씩 예약 시도 (재고 100)
//
// 기대 결과 (락이 없으므로):
//   - reservation_confirmed 가 100을 초과 → oversell 재현 성공
//   - teardown의 STATS에서 remainingQuantity가 음수일 수 있음
//
// 락 적용 단계(Step 1~)에서도 동일 스크립트로 측정해 전/후를 비교한다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const PRODUCT_ID = 1;
const INITIAL_STOCK = 100;

const confirmed = new Counter('reservation_confirmed'); // 200
const rejected = new Counter('reservation_rejected');   // 409 (매진)
const errors = new Counter('reservation_errors');       // 그 외 (5xx 등)

export const options = {
  scenarios: {
    flash_sale: {
      executor: 'per-vu-iterations',
      vus: 1000,        // 동시 사용자 1,000명
      iterations: 1,    // 1인 1건
      maxDuration: '2m',
    },
  },
};

export function setup() {
  const res = http.post(`${BASE_URL}/api/reset?productId=${PRODUCT_ID}&quantity=${INITIAL_STOCK}`);
  if (res.status !== 200) {
    throw new Error(`reset failed: ${res.status} ${res.body}`);
  }
}

export default function () {
  const payload = JSON.stringify({ userId: __VU, productId: PRODUCT_ID });
  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status === 200) {
    confirmed.add(1);
  } else if (res.status === 409) {
    rejected.add(1);
  } else {
    errors.add(1);
  }

  check(res, {
    'no server error': (r) => r.status === 200 || r.status === 409,
  });
}

export function teardown() {
  const res = http.get(`${BASE_URL}/api/stats?productId=${PRODUCT_ID}`);
  console.log(`\n===== STATS =====\n${res.body}\n=================`);
  const stats = JSON.parse(res.body);
  if (stats.confirmedCount > INITIAL_STOCK) {
    console.log(`>>> OVERSELL: ${stats.confirmedCount - INITIAL_STOCK}건 초과 판매 (재고 ${INITIAL_STOCK} / 확정 ${stats.confirmedCount})`);
  } else {
    console.log('>>> oversell 없음');
  }
}
