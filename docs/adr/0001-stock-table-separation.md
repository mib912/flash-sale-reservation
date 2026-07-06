# ADR-0001: 재고를 Product에서 분리한 Stock 테이블 + raw FK

## 상태
채택 (Step 0부터 적용)

## 맥락
선착순 예약에서 경쟁이 몰리는 자원은 "재고 수량" 하나다. 이 카운터를
어디에 둘지가 락 범위(lock granularity)를 결정한다.

## 후보안
- (A) Product 테이블에 quantity 컬럼을 두고 직접 차감
- (B) 재고를 별도 Stock 테이블로 분리, Product는 정적 카탈로그로만
- 연관관계: JPA `@ManyToOne` vs raw `Long productId`

## 선택
- 재고를 **별도 Stock 테이블**로 분리 (B)
- Stock.productId는 **raw Long** (JPA 연관관계 없음)

## 버린 이유 / 트레이드오프
- (A)를 버린 이유: Product(이름·가격 등 정적 정보)에 락을 걸면 재고와 무관한
  읽기까지 잠긴다. 재고를 분리하면 **잠기는 범위를 재고 행 하나로 한정**할 수 있다
  (Step 1 비관락에서 이 효과가 직접 드러남).
- 연관관계 대신 raw FK: 동시성 핫패스에서 불필요한 조인/프록시 로딩/N+1을 피한다.
  대가로 FK 제약과 객체 그래프 탐색(stock.getProduct())은 포기.
- Product에 quantity를 중복 저장하지 않음 (단일 진실 소스). 총 수량과 남은 수량이
  뒤섞이는 버그를 원천 차단.

## 측정값
- 이 결정 자체는 성능 수치보다 **락 범위**에 대한 것. Step 1에서 재고 행만 잠그는
  효과로 확인.
