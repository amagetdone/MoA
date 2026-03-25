# 카드 거래내역 초기화 성능 분석 및 개선

## 문제 상황

- `/admin/data-init/card` 실행 시 카드 거래내역 초기화가 이전보다 눈에 띄게 느려졌다.
- 외부 추론은 기존에도 존재했고 현재 요구상 유지되어야 하므로, 저장 경로 병목 여부를 우선 분석했다.
- 계측은 H2 기반 로컬 테스트 하네스인 `InitializeCardTransactionOverridesUseCasePerformanceTest`로 동일한 690건 입력을 사용해 수행했다.
- 측정 기준:
  - 기존 저장 경로: `CardTransactionOverrideRepository.saveAll(...)`
  - 개선 저장 경로: `CardTransactionOverrideBulkSaveRepository.saveAll(...)`
- 로컬 계측 결과:
  - 기존 로직: `0.344초`
  - 개선 로직: `0.031초`
  - 개선 폭: 약 `11.1배` 빨라짐, 약 `90.99%` 감소

## 원인

- 핵심 원인은 외부 추론이 아니라 DB 저장 방식이었다.
- `CardTransactionOverride`는 PK를 애플리케이션에서 미리 계산해서 넣는다.
- Spring Data JPA의 `saveAll`은 PK가 이미 채워진 엔티티를 신규 insert가 아니라 기존 엔티티 후보로 보고 `merge` 경로를 타기 쉽다.
- 이 엔티티는 `TransactionOverride -> CardTransactionOverride`의 `JOINED` 상속 구조라서, 한 건 저장 시:
  - 존재 확인 성격의 추가 SQL
  - 부모 테이블 insert
  - 자식 테이블 insert
  가 반복된다.
- 계측 결과 기존 경로는 690건 저장에 `prepared statement 2,070개`를 사용했다. 건당 3개 수준의 SQL이 발생한 셈이라 round-trip 비용이 커졌다.

## 다양한 접근방법

### 1. JPA `saveAll` 유지 + Hibernate batch 설정 추가

- 장점: 변경 범위가 작다.
- 단점: 수동 PK + `JOINED` 상속 구조에서 `merge` 자체가 남아 select/flush 오버헤드를 완전히 제거하지 못한다.

### 2. 카드 초기화 전용 JDBC batch insert 경로 추가

- 장점: 부모/자식 테이블 insert를 직접 제어할 수 있어 JPA `merge` 오버헤드를 제거할 수 있다.
- 장점: 외부 추론, 시드 생성, 삭제 로직은 그대로 두고 저장 경로만 교체할 수 있다.
- 단점: 저장 전용 포트/저장소가 하나 더 생긴다.

### 3. staging/COPY 기반 대량 적재

- 장점: 더 빠를 수 있다.
- 단점: 현재 690건 규모에는 과하고 구현 복잡도가 높다.

## 선택한 해결방법

- 2번을 선택했다.
- 변경 내용:
  - `InitializeCardTransactionOverridesUseCase`가 `JpaRepository.saveAll` 대신 `CardTransactionOverrideBulkSavePort`를 호출하도록 분리
  - `CardTransactionOverrideBulkSaveRepository`를 추가해 `JdbcTemplate.batchUpdate(...)`로
    - `transaction_override`
    - `card_transaction_override`
    두 테이블에 batch insert 수행
- 유지한 것:
  - 외부 추론 호출
  - 기존 카드 시드
  - 기존 분석 결과 삭제 및 카드 거래 override 삭제 흐름
- 추가 검증:
  - `CardTransactionOverrideBulkSaveRepositoryTest`
  - `InitializeCardTransactionOverridesUseCaseTest`
  - `InitializeCardTransactionOverridesUseCasePerformanceTest`

## 결과

- 동일한 690건 저장 기준으로 `0.344초 -> 0.031초`로 줄었다.
- 병목은 외부 추론이 아니라 `saveAll`의 `merge` 기반 저장 경로와 `JOINED` 상속으로 인한 과도한 SQL round-trip이었다.
- 개선 후에는 카드 초기화 저장이 두 번의 JDBC batch insert로 정리되어 저장 비용이 크게 줄었다.
- 측정 명령:

```bash
./gradlew test --tests "*InitializeCardTransactionOverridesUseCasePerformanceTest"
```

- 성능 로그:

```text
CARD_INIT_BENCHMARK baseline_jpa=0.344s improved_jdbc=0.031s jpa_prepared_statements=2070 rows=690
```
