# flash-sale-reservation

한정 재고에 다수 사용자가 동시에 몰릴 때 **재고 정합성(oversell 0)** 을 보장하는
동시성 처리 시스템. 락 전략을 단계적으로 적용하고 **동일 부하로 측정**해
"정합성을 얻는 대가(처리량·지연)"를 정량 비교한다.

> 타겟: 티빙 Backend Engineer (Billing Platform) — 대규모 결제 트랜잭션 정합성.
> "재고 정합성 oversell 0"은 "결제 트랜잭션 정합성"과 같은 근육이다.

---

## 실험 조건 (모든 Step 고정)

| 항목 | 값 |
|---|---|
| 초기 재고 | 100 |
| 동시 사용자 (k6 VU) | 1,000 |
| 요청 | 1인 1건 (per-vu-iterations, iterations=1) |
| DB 커넥션 풀 (HikariCP) | 100 |
| 측정 지표 | oversell 건수 · TPS · p95 응답시간 |

> 조건이 바뀌면 Step 간 수치를 비교할 수 없다. 위 조건을 고정하고 모든 Step을 같은 부하로 측정한다.

---

## Step별 결과 비교 (핵심 산출물)

| Step | 전략 | confirmed | oversell | remaining | 재시도 | TPS | p95(전체) | p95(성공) |
|---|---|---|---|---|---|---|---|---|
| 0 | 락 없음 (naive) | 1000 | **900** | 89 | — | ~436/s | 1.66s | — |
| 1 | DB 비관락 (`PESSIMISTIC_WRITE`) | 100 | **0** | 0 | 0 | **~687/s** | **1.2s** | **492ms** |
| 2 | DB 낙관락 (`@Version` + 재시도) | 100 | **0** | 0 | **9,676** | ~80/s | 12.17s | 10.44s |

> **Step 0의 불일치가 lost update의 증거**: confirmed(1000) ≠ 초기재고−remaining(100−89=11).
> 1000번 차감했지만 서로 덮어써 실제 반영은 11번뿐 → 재고 89, 그런데 예약은 전부 성공.

### 결론: 이 도메인에는 비관락
- 비관락·낙관락 **둘 다 정합성은 동일**(oversell 0, confirmed 100). 차이는 성능.
- 낙관락은 재고 100개를 파는 데 **재시도 9,676회** 발생(1개당 ~97회 충돌) → TPS 1/8, p95 10배.
- 모두가 단일 재고 행을 다투는 **고경합**이라 낙관락 최악 조건. → **비관락 채택.**
- 단, "낙관락이 항상 나쁨"게 아니라 "이 시나리오에서 나쁨" 것. 저경합이면 낙관락이 유리.

> ⚠️ **TPS·p95의 Step0↔Step1 직접 비교는 부당.** 두 버전이 한 "일의 양"이 다름
> (Step0은 1000건 전부 DB 쓰기, Step1은 100건만 쓰고 900건 즉시 거절). 상세: ADR-0003.
> 정합성 비교(oversell 900→0)만이 공정하며, 그것이 이 프로젝트의 본질이다.

---

## 왜 이 설계인가 (면접 방어 소스)

각 결정의 맥락·후보안·트레이드오프·측정값은 `docs/adr/` 에 기록.

- [ADR-0001] 재고를 `Stock` 테이블로 분리 + raw FK — lock granularity 최소화
- [ADR-0002] HikariCP 풀 100 — baseline 동시성 확보, 병목이 풀 대기→행 락 경합으로 이동
- [ADR-0003] 락 전략 선택 (비관 vs 낙관) — Step 1·2 비교로 결론

---

## 실행 순서

```bash
docker compose up -d          # MySQL
./gradlew bootRun             # 앱 (localhost:8080)
k6 run load-test/step0.js     # 부하 테스트
# 결과: 콘솔 STATS 블록 + k6 summary(http_req_duration p95, http_reqs/s)
```

Step 브랜치: `feature/step0-naive-baseline`, `feature/step1-pessimistic`, `feature/step2-optimistic`
(각 step은 step0에서 분기 — 서로 독립 대안이므로 순차 누적하지 않는다.)
