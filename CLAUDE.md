# flash-sale-reservation

선착순 예약 동시성 처리 시스템. 한정 재고에 다수 사용자가 동시에 몰릴 때
**재고 정합성(oversell 0)** 을 보장하는 것을 핵심 문제로 삼고,
락 전략을 단계적으로 적용·측정하는 백엔드 포트폴리오 프로젝트.

## 목적
- 이직용 백엔드 포트폴리오. "기능 구현"이 아니라 **"동시성·시스템 설계 깊이"** 를 증명.
- 타겟: 티빙 Backend Engineer (Billing Platform 우선 지원).
  → JD 직결: 대규모 결제 트랜잭션 정합성, RDB 모델링·최적화, 테스트 코드.
  → 서사 리프레이밍: "재고 정합성 oversell 0" = "결제 트랜잭션 정합성" (같은 근육).
  → 우대요건 매칭 여지: Kafka/MQ 비동기(Step 4), Low Latency(락 전략별 p95 비교).
  → 갭: JD의 LLM/AI Agent·RAG 경험은 이 프로젝트로 못 메움 — 별도 트랙.
  (원 타겟: CJ올리브영 커머스 — 동시성·정합성 근육은 동일해 그대로 유효.)
- 면접에서 **"왜 이 설계를 했는지" 설명 가능한 수준**까지 깊게 가는 게 전부.

## 작업 원칙 (반드시 지킬 것)
- **설계 결정은 사용자(개발자)가 한다.** Claude는 선택지와 트레이드오프를 제시하고
  면접관처럼 반박한다. 정답을 먼저 주지 않는다.
- **Claude는 보일러플레이트 담당**, 사용자는 설계·동시성 로직·"왜"를 소유한다.
- **항상 설명 먼저, 코드 나중.** 사용자는 JPA를 처음 쓴다 → 문법을 그때그때 설명한다.
- 문서·API 스펙을 예쁘게 만드는 데 시간 쓰지 않는다. 코드로 문제를 재현하고 숫자로 증명.

## 성공 기준 (정량)
재고 **100**, 동시 **1,000** 요청, 1인 1건 → 정확히 100건 성공, **oversell 0, 중복 0**.
각 단계에서 동일 부하로 측정: 성공/oversell/중복 건수, TPS, p95 응답시간.

## 단계별 로드맵
- **Step 0** (현재): 락 없는 naive 구현 → 부하 테스트로 oversell 재현 ("터지는 baseline")
- **Step 1**: DB 비관락 (`@Lock(PESSIMISTIC_WRITE)`)
- **Step 2**: DB 낙관락 (`@Version`) — 비관락과 처리량/실패율 비교
- **Step 3**: Redis 원자적 차감(DECR/Lua) / 분산락
- **Step 4**: Kafka 비동기 확정 + 멱등성 + DLQ

## 기술 스택
- Java 21, Spring Boot 3.3.x, Gradle (Kotlin DSL)
- Spring Data JPA + MySQL 8
- (Step 3) Redis · (Step 4) Kafka
- 부하 테스트: k6
- 인프라: docker-compose (단계별로 서비스 추가, 미리 다 띄우지 않음)

## 컨벤션
- 패키지: `com.flashsale.*` (전부 소문자)
- **풍부한 도메인 모델**: setter 금지. 상태 변경은 의미 있는 메서드(`decrease()` 등)로만.
- 기본 생성자는 `@NoArgsConstructor(access = PROTECTED)` — JPA용으로만 열고 외부 차단.
- 엔티티 간 참조는 JPA 연관관계 대신 **raw FK id**(`Long productId`) 사용.

## 설계 결정 로그 (면접 방어용)
- **재고를 Product가 아닌 별도 `Stock` 테이블로 분리**
  → 경쟁이 몰리는 카운터를 정적 카탈로그(Product)에서 격리.
  → Step 1에서 락을 걸 때 잠기는 범위를 재고 행으로 한정 (lock granularity 최소화).
- **`Stock.productId`는 raw Long** (연관관계 X)
  → 동시성 핫패스에서 불필요한 조인/프록시/N+1 회피. 대가로 FK 제약·객체 탐색은 포기.
- **Product에 quantity 중복 금지** (단일 진실 소스). 총 수량을 깎으면 "남은 재고"와 뒤섞임.
- **HikariCP 커넥션 풀 100으로 확대** (기본 10 → 100)
  → baseline에서 DB 동시성을 실제로 확보하기 위함. 풀이 10이면 VU 1,000을 던져도
    DB에 동시 도달하는 요청이 10으로 눌려 oversell(read-check-write 겹침)이 약하게 재현된다.
  → 트레이드오프: 풀을 키우면 병목이 "커넥션 풀 대기"에서 "DB 행 락 경합"으로 이동한다.
    즉 동시성 강도는 풀 크기가 지배한다 — 이 사실 자체를 Step 0의 관찰 포인트로 삼는다.
  → 전제: MySQL 8 기본 max_connections=151 이라 100은 수용 가능. 풀을 200+ 로 올리면
    docker-compose의 MySQL max_connections도 함께 올려야 한다.

## 도메인
- `Product` (id, name, price) — 정적 카탈로그. 재고 없음.
- `Stock` (id, productId, quantity) — 가변 재고. 동시성 제어의 대상.
- `Reservation` (id, userId, productId, status, createdAt) — 예약. status: CONFIRMED/FAILED.

## 실행 순서
1. `docker compose up -d` (MySQL)
2. `./gradlew bootRun`
3. 재고 리셋: `POST /api/reset` (부하 테스트 반복 전마다)
4. `k6 run load-test/step0.js`
5. 결과 확인: `GET /api/stats?productId=1` → 성공 수가 재고(100)를 넘으면 oversell 재현 성공

## 참고
- JPA 문법 정리: `docs/jpa-노트.md`
- 기획/설계 원본: Notion "선착순 예약 동시성 처리 시스템"
