# Admin Data Init AI Category Design

## 배경

현재 `/admin/data-init`는 SSAFY 더미 계좌/카드/거래를 만든 뒤 로컬 override 거래를 저장할 때 모든 거래에 기본 카테고리 `기타서비스(id=4)`를 고정으로 넣는다.

이 방식은 더미 데이터 생성 직후 거래 카테고리 품질이 낮고, 분석 결과가 실제 운영 흐름과 다르게 보이는 문제가 있다.

## 목표

- `/admin/data-init`에서 생성하는 계좌 거래와 카드 거래를 저장할 때 AI 서버 분류 결과를 사용한다.
- 계좌 거래는 `transactionSummary` 우선, 비면 `transactionMemo`를 AI 입력으로 사용한다.
- 카드 거래는 `merchantName`을 AI 입력으로 사용한다.
- AI 호출 실패, 응답 누락, 카테고리 매핑 실패 시 `기타서비스(id=4)`로 fallback 한다.

## 외부 의존

- AI API: `POST https://j14d101.p.ssafy.io/inference/v1/merchant/batch`
- 요청:

```json
{
  "merchant_texts": ["string"]
}
```

- 응답:

```json
{
  "results": [
    {
      "merchant_text": "string",
      "normalized_merchant_text": "string",
      "raw_label": "string",
      "final_label": "string",
      "confidence": 0,
      "fallback_applied": true,
      "threshold_used": 0
    }
  ]
}
```

## 선택지

### 1. `/admin/data-init` 내부에서 직접 AI 분류 후 저장

- 더미 거래 생성 직후 AI 서버를 한 번 더 호출한다.
- 응답의 `final_label`을 `category.name`으로 매핑해서 저장한다.
- 지금 요구 범위에 가장 잘 맞고 변경량이 작다.

### 2. 공용 카테고리 분류 서비스로 추상화

- 분석 로직과 관리자 더미 초기화를 모두 같은 분류 서비스가 쓰게 만든다.
- 장기적으로는 좋지만 이번 수정 범위에는 과하다.

### 3. 기본 카테고리로 저장 후 후처리 업데이트

- 저장 이후 카테고리를 다시 수정한다.
- 같은 트랜잭션에서 불필요한 write가 두 번 발생한다.

## 결정

1번을 채택한다.

## 데이터 흐름

1. `/admin/data-init`가 SSAFY 계좌/카드/거래를 생성한다.
2. 계좌 거래 텍스트 목록을 추출한다.
   - `transactionSummary` 우선
   - 비어 있으면 `transactionMemo`
3. 카드 거래 텍스트 목록을 추출한다.
   - `merchantName`
4. 계좌 거래 배치 1회, 카드 거래 배치 1회로 AI 서버를 호출한다.
5. 응답의 `final_label`을 로컬 `category.name`과 매칭한다.
6. 매칭 성공 시 해당 카테고리로 저장한다.
7. 매칭 실패, 응답 누락, AI 예외 시 `기타서비스(id=4)`로 저장한다.

## 구현 범위

- `InitializeSsafyDummyDataUseCase`
  - `MerchantInferenceApiClient` 주입
  - AI 요청용 텍스트 추출 로직 추가
  - 응답 `final_label -> Category` 매핑 로직 추가
  - 계좌/카드 거래 저장 시 category 선택 로직 변경
- 테스트
  - 정상 분류 저장
  - AI 실패 fallback
  - 일부 라벨 매핑 실패 fallback

## 예외 처리

- AI 서버 예외는 `/admin/data-init` 전체 실패 사유가 아니다.
- SSAFY 생성과 로컬 저장만 성공하면 엔드포인트는 성공한다.
- AI 실패 시 해당 거래들의 category만 `기타서비스`로 저장한다.

## 기대 효과

- 더미 거래가 생성 즉시 분류된 상태로 저장된다.
- 분석 API와 관리자 더미 데이터의 카테고리 품질 차이가 줄어든다.
- AI 서버 장애가 있어도 관리자 더미 초기화 자체는 계속 사용할 수 있다.
