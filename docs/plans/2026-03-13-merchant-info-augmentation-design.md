# Merchant Info Augmentation Design

**Date:** 2026-03-13

## Goal

`AI/lab/koelectra_merchant_training_kfold.ipynb`가 기존 `train.csv`, `valid.csv`, `test.csv`만 사용하는 대신, `AI/lab/merchant_info` 아래의 소상공인시장진흥공단 상권 CSV와 `소분류코드_소비카테고리_매핑.csv`를 읽어 약지도 학습 데이터를 증강하도록 확장한다.

## Scope

- `상호명`만 사용해 추가 학습 샘플을 만든다.
- `상권업종소분류코드 -> 소비카테고리` 매핑으로 라벨을 붙인다.
- 새 데이터는 학습용으로만 사용하고, 기존 `valid/test` 홀드아웃은 그대로 유지한다.
- 증강 데이터 생성 로직은 테스트 가능한 파이썬 모듈로 분리하고, 노트북은 이 모듈을 호출한다.

## Data Design

- 입력 원본:
  - `AI/lab/data/merchant_category_dataset_v1/{train,valid,test}.csv`
  - `AI/lab/merchant_info/소상공인시장진흥공단_상가(상권)정보_*.csv`
  - `AI/lab/merchant_info/소분류코드_소비카테고리_매핑.csv`
- 상권 CSV에서 사용할 컬럼:
  - `상호명`
  - `상권업종소분류코드`
- 매핑 CSV에서 사용할 컬럼:
  - `상권업종소분류코드`
  - `소비카테고리`
- 생성 컬럼:
  - `sample_id`
  - `split`
  - `category_id`
  - `category_name`
  - `merchant_family_id`
  - `merchant_family`
  - `merchant_text`
  - `normalized_merchant_text`
  - `family_type`
  - `family_source`
  - `variant_source`

## Processing Rules

1. 상권 CSV들을 모두 합친다.
2. `상호명`이 비어 있거나 공백뿐인 행은 제거한다.
3. `상권업종소분류코드` 기준으로 매핑 CSV를 붙인다.
4. 기존 데이터셋의 `category_name` 집합에 없는 카테고리는 제거한다.
5. `normalized_merchant_text`는 기존 노트북의 서비스용 정규화 규칙과 동일한 NFKC + 공백 정리 + casefold 규칙으로 생성한다.
6. 동일한 `merchant_text`, `category_name` 조합은 중복 제거한다.
7. 기존 데이터셋에 이미 같은 `merchant_text`, `category_name` 조합이 존재하면 추가 데이터에서 제외한다.
8. 새 데이터는 셔플 후 전부 `train` split으로만 넣고, `valid/test`는 변경하지 않는다.

## Notebook Integration

- 노트북 초반 데이터 로드 단계 직후에 증강 단계를 추가한다.
- 증강 전후 row 수와 카테고리 분포를 출력한다.
- 증강 결과는 재사용 가능하도록 별도 CSV로 저장한다.
- 이후 `build_input_text`, `GroupKFold`, 최종 학습/평가 흐름은 그대로 유지한다.
- `valid/test`는 원본 홀드아웃 그대로 유지해 약라벨 평가 오염을 막는다.

## Risks And Mitigations

- 라벨 노이즈:
  - 소분류 단위 매핑은 거칠 수 있으므로 기존 홀드아웃 평가를 유지한다.
- 데이터 편향:
  - 특정 업종이 과도하게 많을 수 있으므로 카테고리별 증강 건수를 출력한다.
- 노트북 수정 리스크:
  - 로직을 별도 모듈로 분리해 `.ipynb` 변경 범위를 최소화한다.

## Testing Strategy

- 매핑/정규화/분할/중복 제거를 단위 테스트로 검증한다.
- 노트북에서는 증강 결과 shape와 저장 경로 출력으로 동작을 확인한다.
