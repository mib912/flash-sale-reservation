# JPA 기초 노트 (이 프로젝트에서 쓰는 것만)

JPA = 자바 객체와 DB 테이블을 자동으로 매핑해주는 표준. 직접 SQL을 짜는 대신
객체를 다루면 JPA가 그에 맞는 SQL(INSERT/SELECT/UPDATE)을 만들어 실행한다.
(실무에서 쓰던 MyBatis는 SQL을 직접 쓰는 방식 → JPA는 그 SQL을 가려준다는 게 큰 차이.)

Spring에서는 보통 **Hibernate**라는 구현체가 실제로 동작한다.

---

## 1. 엔티티 매핑 (Product.java / Stock.java)

```java
@Entity                                  // 이 클래스 = DB 테이블 하나
public class Stock {

    @Id                                  // 이 필드 = 기본키(PK)
    @GeneratedValue(strategy = IDENTITY) // PK 생성을 DB에 위임 (MySQL auto_increment)
    private Long id;

    private Long productId;              // 컬럼명 productId 로 자동 매핑
    private int quantity;
}
```

- `@Entity`: JPA가 관리하는 객체임을 표시. 클래스 이름 → 테이블 이름(기본).
- `@Id`: 기본키 필드.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: PK 값을 DB가 자동 증가로
  채우게 한다. (다른 전략: SEQUENCE, TABLE 등. MySQL은 보통 IDENTITY.)
- 필드는 별도 애너테이션 없으면 같은 이름의 컬럼으로 매핑된다.
  (명시하려면 `@Column(name = "...")`.)

### @NoArgsConstructor(access = PROTECTED) 가 왜 필요한가
JPA는 DB에서 데이터를 읽어 객체를 만들 때 **기본 생성자(인자 없는 생성자)** 를
리플렉션으로 호출한다. 그래서 반드시 있어야 한다.
하지만 `public`으로 열면 아무 데서나 `new Stock()`으로 빈 객체를 만들 수 있어
일관성이 깨진다. → `PROTECTED`로 막고, 의미 있는 생성자만 외부에 공개한다.

### Lombok
- `@Getter`: 모든 필드의 getter를 컴파일 시 자동 생성 (코드만 안 보일 뿐 실제로 있음).
- `@NoArgsConstructor(access = ...)`: 기본 생성자 자동 생성 + 접근 범위 지정.
- **setter는 일부러 안 만든다.** 상태 변경은 `decrease()` 같은 의미 있는 메서드로만.

---

## 2. 리포지토리 (StockRepository.java)

```java
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductId(Long productId);
}
```

- `JpaRepository<엔티티, PK타입>` 을 상속하면 **기본 CRUD 메서드가 공짜**로 생긴다:
  `save()`, `findById()`, `findAll()`, `delete()`, `count()` 등. 구현체는 Spring이 자동 생성.
- **쿼리 메서드(Query Method)**: 메서드 *이름*만 규칙대로 지으면 SQL이 자동 생성된다.
  - `findByProductId(Long id)` → `SELECT * FROM stock WHERE product_id = ?`
  - `countByProductId(Long id)` → `SELECT count(*) FROM ... WHERE product_id = ?`
  - `deleteByProductId(Long id)` → `DELETE FROM ... WHERE product_id = ?`
- 반환 타입 `Optional<Stock>`: 결과가 없을 수 있음을 타입으로 표현. `.orElseThrow(...)`로 처리.

---

## 3. 트랜잭션과 "변경 감지" (ReservationService.java)

```java
@Transactional
public boolean reserve(Long userId, Long productId) {
    Stock stock = stockRepository.findByProductId(productId).orElseThrow(...);
    stock.decrease();   // ← save()를 호출하지 않았는데도 DB에 반영된다!
    reservationRepository.save(new Reservation(...));
    return true;
}
```

여기서 **JPA의 가장 헷갈리는 핵심** 두 가지:

### (1) @Transactional
- 메서드 전체를 하나의 DB 트랜잭션으로 묶는다. 중간에 예외가 나면 전부 롤백.
- 트랜잭션이 살아있는 동안 조회한 엔티티는 JPA가 "관리(영속 상태)"한다.

### (2) Dirty Checking (변경 감지)
- 트랜잭션 안에서 조회한 엔티티의 필드를 바꾸면(`stock.decrease()`),
  **트랜잭션이 끝날 때 JPA가 바뀐 걸 감지해서 자동으로 UPDATE를 날린다.**
- 그래서 `stockRepository.save(stock)`를 명시적으로 부르지 않아도 반영된다.
  (새로 만든 Reservation은 아직 관리 대상이 아니라서 `save()`로 직접 저장해야 함.)

> ⚠️ 바로 이 "조회 → 필드 변경 → 자동 UPDATE" 흐름에서, 락이 없으면
> 여러 트랜잭션이 같은 값을 동시에 읽고 각자 UPDATE해서 **lost update(덮어쓰기)** 가
> 발생한다. 이게 Step 0에서 재현할 oversell의 원리다.

---

## 4. 스키마 자동 생성 (application.yml)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create   # 앱 기동 시 테이블을 매번 새로 만든다 (기존 데이터 삭제)
```

- `ddl-auto: create` → 엔티티 클래스를 보고 테이블을 자동 생성. 기동 때마다 초기화.
  부하 테스트를 깨끗한 상태에서 반복하려고 이 프로젝트는 create를 쓴다.
- 실무 운영에선 보통 `validate`나 `none`을 쓰고 스키마는 따로 관리한다. (지금은 학습용.)

---

## 5. 앞으로 나올 것 (지금은 몰라도 됨, 단계 오면 설명)

- **`@Lock(LockModeType.PESSIMISTIC_WRITE)`** (Step 1): 조회 시 해당 행에 DB 락을 걸어
  다른 트랜잭션이 못 건드리게 함 → `SELECT ... FOR UPDATE`.
- **`@Version`** (Step 2): 낙관락. 엔티티에 버전 컬럼을 두고, UPDATE 시 버전이 바뀌었으면
  충돌로 보고 예외 발생 → 재시도.
