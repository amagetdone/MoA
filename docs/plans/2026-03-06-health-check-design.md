# Health Check Design

작성일: 2026-03-06

## 목표

`BE/amagetdone` 스프링 프로젝트에 전역 헬스체크 컨트롤러를 추가한다.
엔드포인트는 `GET /health` 이며, HTTP 200과 plain text `Hello World!`를 반환한다.

## 선택한 접근

### 1. 전용 전역 컨트롤러 추가

- 패키지 위치는 `com.ssafy.amagetdone.global.controller`로 둔다.
- `@RestController`와 `@GetMapping("/health")`를 사용한다.
- 응답은 DTO 없이 문자열을 직접 반환한다.

이 접근을 선택한 이유는 현재 프로젝트가 초기 상태이고, 요구사항이 단일 고정 응답이기 때문이다.
Actuator 도입이나 JSON 응답 DTO는 지금 시점에서는 불필요하다.

## 컴포넌트 설계

### 컨트롤러

- 클래스명: `GlobalHealthCheckController`
- 책임: 서버 생존 여부를 확인할 수 있는 최소 엔드포인트 제공

### 테스트

- `MockMvc` 기반 웹 계층 테스트를 추가한다.
- 검증 항목:
  - `GET /health` 요청 시 HTTP 200
  - 응답 본문이 `Hello World!`

## 오류 처리

별도 오류 처리 로직은 두지 않는다.
해당 엔드포인트는 외부 의존성이 없는 정적 응답이므로 실패 가능성을 최소화한다.

## 완료 기준

- `/health` 엔드포인트가 생성된다.
- 테스트가 먼저 실패한 뒤, 구현 후 통과한다.
