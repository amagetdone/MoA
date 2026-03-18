# API Docs

이 디렉토리는 현재 백엔드 API 계약을 수동 테스트하기 쉽게 정리한 로컬 명세서 모음이다.

## Base URL

- 모든 Full URL은 배포 서버 기준으로 표기한다.
- Base URL: `https://j14d101.p.ssafy.io/api`

## Domain Files

- `auth.md`
- `member.md`
- `mypage.md`
- `account.md`
- `account-verification.md`
- `budget.md`
- `plan.md`
- `transaction.md`
- `global.md`

## Format Rules

각 엔드포인트는 아래 항목을 같은 순서로 유지한다.

1. 기능명
2. Method
3. Full URL
4. Path
5. Headers
6. Path Variables
7. Query Parameters
8. Request Body
9. Response Body
10. Error Responses
11. Notes

## Conventions

- `GET` 요청은 `Request Body: 없음`으로 적는다.
- `multipart/form-data` 요청은 `Content-Type`과 form field 이름을 적는다.
- JSON 예시는 가능한 한 controller test의 MockMvc payload를 우선 사용한다.
- 테스트 예시가 없는 경우에는 DTO/컨트롤러 시그니처 기준 최소 예시를 작성하고 Notes에 표시한다.
- 에러 응답은 코드나 기존 테스트/명세에서 확인 가능한 항목만 적는다.
