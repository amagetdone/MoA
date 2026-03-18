# Non-Target Notion API Pages Created Before Scope Clarification

## Summary

2026-03-17 API 명세 정리 작업 중, 사용자 scope가 최종적으로 `account`, `member`, `budget` Notion 업데이트로 좁혀지기 전에 아래 non-target 도메인 Notion API 페이지가 생성/갱신되었다.

이후 Notion 레이아웃은 다시 기존 양식에 맞춰 정리했다.

- `소비 분석 관리` 페이지 안에 `소비 분석 관리 api` inline 데이터베이스를 두고 transaction/analysis 페이지를 그 아래로 이동
- `설정 관리` 페이지 안의 inline 데이터베이스 아래로 `헬스 체크` 페이지를 이동
- 즉, 이 이슈는 이제 "양식 위반" 이슈가 아니라 "scope 밖 도메인까지 Notion에 반영된 상태"를 추적하는 이슈다.

사용자 최종 요구사항은:

- Notion 작업은 `account`, `member`, `budget`만 업데이트
- 그 외 도메인은 로컬 문서(`docs/api/`)와 이슈 문서로 관리

따라서 아래 페이지들은 이번 작업 결과로 남아 있지만, 후속 의사결정이 필요하다.

## Affected Notion Pages

### Plan

- 목표 이미지 업로드
  - https://www.notion.so/32639da7e9f281649f4dc310a7fdc08c
- S3 파일 업로드 테스트
  - https://www.notion.so/32639da7e9f281c3bc30f65acaaeccac
- S3 업로드 파일 삭제 테스트
  - https://www.notion.so/32639da7e9f2812d949aca984d046435

### Transaction / Analysis

- 지출 요약 조회
  - https://www.notion.so/32639da7e9f2816f99c9f6bba9afbe7b
- 월 지출 조회
  - https://www.notion.so/32639da7e9f2814c830dea5d47ceee33
- 거래 내역 목록 조회
  - https://www.notion.so/32639da7e9f281b596b7d1f7fbe034db
- 분석 작업 생성
  - https://www.notion.so/32639da7e9f2815f8612daf8cfc739d5
- 분석 작업 상태 조회
  - https://www.notion.so/32639da7e9f28188b3cfff18de72471f
- 주간 분석 조회
  - https://www.notion.so/32639da7e9f28109b3cfcd9851937c26
- 월간 분석 조회
  - https://www.notion.so/32639da7e9f28184a603fe592698d7f2
- 소비 분석 관리 도메인 페이지
  - https://www.notion.so/31f39da7e9f281b19d90d8dd50396b10
- 소비 분석 관리 inline 데이터베이스
  - https://www.notion.so/3c4119a2d8994913b6e6f43281804671

### Global

- 헬스 체크
  - https://www.notion.so/32639da7e9f281e19ff8eb25b2fe2468

## Recommended Follow-Up

1. 위 non-target 페이지들을 계속 유지할지 결정한다.
2. 유지하지 않을 경우 수동으로 휴지통 처리하거나 로컬 문서만 canonical로 남긴다.
3. 앞으로 Notion API 명세 작업은 scope 확인과 함께 기존 도메인 페이지 + inline 데이터베이스 양식을 먼저 확인한 뒤 진행한다.

## Local Documentation

Notion scope와 별개로, 아래 로컬 문서는 계속 유지한다.

- `docs/api/plan.md`
- `docs/api/transaction.md`
- `docs/api/global.md`
