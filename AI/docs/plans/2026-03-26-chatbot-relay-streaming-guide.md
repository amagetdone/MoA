# MOA Chatbot Relay Streaming Integration Guide

## 목적

이 문서는 Flutter 앱이 Spring Boot 서버를 통해 MOA FastAPI 챗봇을 호출할 때 필요한 작업을 정리한다.

구성 원칙은 아래와 같다.

- Flutter는 Spring Boot만 호출한다.
- Spring Boot는 AccessToken 인증을 수행하고 `userId`를 추출한다.
- Spring Boot는 FastAPI로 요청을 릴레이한다.
- 스트리밍 응답은 Spring Boot가 `text/event-stream`을 그대로 프록시한다.
- FastAPI는 `X-User-Id` 기반으로 사용자 격리를 수행한다.

## 현재 FastAPI 계약

### 1. 일반 응답

`POST /v1/chatbot/messages`

Request body:

```json
{
  "message": "이번 주 예산 어때?",
  "conversation_id": "optional-conversation-id"
}
```

Required header:

```text
X-User-Id: 123
```

Response:

```json
{
  "conversation_id": "generated-or-reused-id",
  "answer": "## 이번 주 예산\n- 자유 예산이 남아 있습니다.",
  "warnings": [],
  "tool_summaries": [
    {
      "tool_name": "get_weekly_budget_status",
      "as_of": "2026-03-26"
    }
  ]
}
```

주의:

- `answer`는 이제 마크다운 문자열이다.
- `conversation_id`가 있으면 해당 대화에 이어서 처리된다.
- `conversation_id`가 없으면 현재 사용자 기준 최신 활성 대화를 재사용하고, 없으면 새로 생성한다.
- 만료된 대화는 `410 Gone`이 반환된다.

### 2. 스트리밍 응답

`POST /v1/chatbot/messages/stream`

Request body는 일반 응답과 동일하다.

Response content-type:

```text
text/event-stream
```

이벤트 예시:

```text
event: status
data: {"message":"routing"}

event: token
data: {"text":"## 이번 주 예산"}

event: token
data: {"text":"\n- 자유 예산이 남아 있습니다."}

event: done
data: {"conversation_id":"conv-123"}
```

주의:

- `token` 이벤트를 이어 붙이면 최종 마크다운 응답이 된다.
- `done` 이벤트의 `conversation_id`를 다음 턴에 재사용하면 가장 명시적이다.
- `conversation_id` 없이 보내더라도 서버가 현재 사용자 기준 최신 활성 대화를 자동으로 찾아 이어 붙인다.
- 만료된 대화는 스트림 시작 전에 `410 Gone`이 반환된다.

### 3. 최신 대화 조회

`GET /v1/chatbot/conversations/latest`

Required header:

```text
X-User-Id: 123
```

Response:

```json
{
  "conversation_id": "conv-123",
  "created_at": "2026-03-26T09:00:00+09:00",
  "last_activity_at": "2026-03-26T09:05:00+09:00",
  "expires_at": "2026-03-29T09:05:00+09:00"
}
```

주의:

- 최신 활성 대화가 있으면 그 `conversation_id`를 반환한다.
- 최신 활성 대화가 없으면 새 `conversation_id`를 발급해 빈 세션을 생성한 뒤 반환한다.
- 프론트는 이 응답 뒤에 `GET /v1/chatbot/conversations/{conversation_id}`로 상세 메시지를 복원하면 된다.

### 4. 대화 조회

`GET /v1/chatbot/conversations/{conversation_id}`

Required header:

```text
X-User-Id: 123
```

Response:

```json
{
  "conversation_id": "conv-123",
  "created_at": "2026-03-26T09:00:00+09:00",
  "last_activity_at": "2026-03-26T09:05:00+09:00",
  "expires_at": "2026-03-29T09:05:00+09:00",
  "messages": [
    {
      "role": "user",
      "content": "이번 주 예산 어때?",
      "created_at": "2026-03-26T09:00:00+09:00"
    },
    {
      "role": "assistant",
      "content": "## 이번 주 예산\n- 자유 예산이 남아 있습니다.",
      "created_at": "2026-03-26T09:00:02+09:00"
    }
  ]
}
```

주의:

- 대화 이력은 마지막 메시지 기준 3일 TTL이다.
- 만료되면 `410 Gone`
- 존재하지 않으면 `404 Not Found`

---

## 백엔드 파트: Spring Boot 작업 정리

### 1. 인증 책임 정리

Spring Boot는 아래 책임을 가진다.

- Flutter가 보낸 AccessToken 검증
- 인증 성공 후 `userId` 추출
- 클라이언트가 임의의 `userId`를 보내지 못하게 차단
- FastAPI 호출 시 `X-User-Id` 헤더를 서버가 직접 설정

정리:

- Flutter -> Spring Boot: `Authorization: Bearer <token>`
- Spring Boot -> FastAPI: `X-User-Id: <authenticated user id>`
- Flutter가 `userId`를 body나 query로 보내는 방식은 사용하지 않는다.

### 2. Spring Boot 외부 API 설계

프론트엔드는 FastAPI 경로를 직접 알 필요가 없도록 Spring Boot에서 relay endpoint를 제공하는 것이 좋다.

권장 경로:

- `POST /api/chatbot/messages`
- `POST /api/chatbot/messages/stream`
- `GET /api/chatbot/conversations/latest`
- `GET /api/chatbot/conversations/{conversationId}`

권장 request/response 계약:

- request body는 FastAPI와 동일하게 유지
- JSON 응답도 FastAPI와 최대한 동일하게 유지
- 상태 코드도 FastAPI를 최대한 그대로 전달

이유:

- Flutter가 Spring/FastAPI의 DTO 차이를 신경 쓰지 않아도 된다.
- 추후 FastAPI가 확장돼도 relay 레이어 변경이 작아진다.

### 3. 일반 JSON relay 구현

권장 방식:

- `WebClient`로 FastAPI `POST /v1/chatbot/messages` 호출
- `Authorization` 헤더는 FastAPI로 넘기지 않고 Spring에서만 사용
- `X-User-Id`만 새로 넣어서 전달

체크리스트:

- body 그대로 전달
- `Content-Type: application/json`
- FastAPI 응답 body 그대로 반환
- FastAPI status code 그대로 반환
- 404/410/422/503 등 에러를 별도 가공하지 말고 우선 pass-through

### 4. 스트리밍 relay 구현

이번 요구사항의 핵심은 `SSE를 재구성하지 않고 그대로 프록시`하는 것이다.

권장 구현 방향:

- Spring Boot에서 FastAPI `POST /v1/chatbot/messages/stream` 호출
- 응답 `Content-Type: text/event-stream`
- upstream body를 버퍼링하지 말고 그대로 downstream으로 전달

권장 기술 방향:

- `WebClient`
- `Flux<DataBuffer>` 또는 raw byte stream pass-through
- controller도 `text/event-stream`으로 응답

중요:

- `SseEmitter`로 이벤트를 다시 조립하지 않는 것이 좋다.
- `status`, `token`, `done` 이벤트를 Spring이 해석해서 재구성하면 relay가 아니라 adapter가 된다.
- 지금 목표는 FastAPI가 만든 SSE frame을 최대한 그대로 프론트까지 보내는 것이다.

### 5. 스트리밍 relay 구현 시 주의사항

반드시 확인할 항목:

- read timeout이 너무 짧지 않은지 확인
- 중간 프록시(Nginx, ALB, ingress)가 버퍼링하지 않는지 확인
- 응답 헤더에 `Cache-Control: no-cache` 적용 고려
- 가능하면 `X-Accel-Buffering: no` 같은 프록시 비버퍼링 설정 검토
- Spring logging/filter가 response body를 읽어버리지 않도록 주의

문제 패턴:

- 로그 인터셉터가 body를 다 읽어서 스트리밍이 끊김
- reverse proxy가 응답을 모아서 한 번에 보냄
- timeout 때문에 `done` 전에 연결이 종료됨

### 6. 대화 조회 relay 구현

Spring Boot는 아래 조회 endpoint를 그대로 relay한다.

- `GET /api/chatbot/conversations/latest`
- `GET /api/chatbot/conversations/{conversationId}`

체크리스트:

- path variable 그대로 전달
- 인증된 `userId`를 `X-User-Id`로 전달
- 응답 body/status code pass-through

### 7. Spring Boot 에러 처리 정책

권장 정책은 “우선 pass-through, 프론트 문구는 프론트에서 처리”다.

대표 상태 코드:

- `200 OK`: 정상
- `404 Not Found`: 특정 `conversation_id` 상세 조회 시 대화 없음
- `410 Gone`: 대화 만료
- `422 Unprocessable Entity`: 잘못된 요청
- `503 Service Unavailable`: AI 서버 초기화 안 됨

백엔드에서 할 일:

- status code 유지
- 필요하면 공통 에러 body를 감싸지 말고 그대로 전달
- 운영 로그에는 `userId`, `conversationId`, upstream status 정도만 남기고 민감한 prompt/body 전체는 남기지 않기

### 8. Spring Boot 테스트 체크리스트

- 인증 성공 시 `X-User-Id`가 FastAPI로 전달되는지
- 인증 실패 시 FastAPI 호출 자체가 안 되는지
- `/messages` relay가 body/status를 그대로 전달하는지
- `/messages/stream` relay가 token 단위로 흘러가는지
- `/conversations/{id}` relay가 404/410을 그대로 전달하는지
- reverse proxy 환경에서도 버퍼링 없이 동작하는지

---

## 프론트엔드 파트: Flutter 작업 정리

### 1. 호출 대상 정리

Flutter는 FastAPI가 아니라 Spring Boot만 호출한다.

호출 경로:

- `POST /api/chatbot/messages`
- `POST /api/chatbot/messages/stream`
- `GET /api/chatbot/conversations/latest`
- `GET /api/chatbot/conversations/{conversationId}`

Flutter가 할 일:

- AccessToken을 Spring Boot로 전송
- `conversation_id`를 클라이언트 상태로 유지
- assistant 응답은 마크다운 렌더링

Flutter가 하지 말아야 할 일:

- `X-User-Id` 직접 세팅
- FastAPI 직접 호출
- 인증 로직 우회

### 2. 일반 응답 처리

일반 질문 응답 흐름:

1. 현재 `conversation_id`가 있으면 body에 포함
2. 없으면 `message`만 전송해도 서버가 현재 사용자 기준 최신 활성 대화를 자동으로 찾아 이어 붙인다.
3. 활성 대화가 아예 없으면 서버가 새 `conversation_id`를 생성한다.
4. 응답의 `conversation_id`를 로컬 상태에 저장
4. `answer`를 assistant 메시지로 렌더링

권장 저장 상태:

- 현재 활성 `conversation_id`
- 채팅 메시지 목록
- 요청 중 여부
- 최근 에러 상태

### 3. 스트리밍 응답 처리

Flutter는 `text/event-stream`을 읽을 수 있어야 한다.

핵심 처리 순서:

1. 사용자 메시지를 먼저 UI에 추가
2. Spring Boot `/api/chatbot/messages/stream` 호출
3. `status` 이벤트는 로딩 상태 정도로만 처리
4. `token` 이벤트는 임시 assistant 버블에 이어 붙이기
5. `done` 이벤트가 오면 `conversation_id` 갱신 후 메시지 확정

이벤트 의미:

- `status`: 내부 진행 상태
- `token`: assistant 마크다운 본문 일부
- `done`: 스트림 종료와 conversation 확정

### 4. Flutter에서 SSE 처리 시 주의사항

권장 구현 포인트:

- HTTP 스트림을 줄 단위로 읽고 SSE frame 파싱
- `event:`와 `data:`를 기준으로 frame 조립
- 연결 종료/예외 시 현재까지 받은 임시 텍스트 상태 보존

패키지 선택은 팀 스택에 따라 다를 수 있지만, 중요한 건 “SSE를 실제 스트림으로 소비”하는 것이다.

주의:

- 일반 `dio`/`http` JSON 호출과 같은 방식으로 처리하면 안 된다.
- 응답이 끝난 뒤 한 번에 받는 구조가 되면 스트리밍 UX가 사라진다.

### 5. 마크다운 렌더링 처리

assistant 응답은 마크다운 문자열이므로 렌더러가 필요하다.

권장 UX:

- 스트리밍 중에는 임시 텍스트 그대로 보여주기
- `done` 이후 최종 assistant 메시지를 마크다운 위젯으로 다시 렌더링

이유:

- 스트리밍 중간에는 마크다운 문법이 아직 닫히지 않았을 수 있다.
- 토큰마다 마크다운 파싱을 다시 하면 깜빡임이 생길 수 있다.

즉:

- in-progress: plain text rendering
- completed: markdown rendering

### 6. 앱 재진입 시 대화 복원

권장 흐름:

1. 앱 재진입 시 먼저 Spring Boot `/api/chatbot/conversations/latest` 호출
2. 응답에서 현재 사용자 기준 활성 `conversation_id` 확보
3. 이어서 Spring Boot `/api/chatbot/conversations/{conversationId}` 호출
4. 성공 시 전체 transcript 재구성
5. 상세 응답의 `messages`가 비어 있으면 아직 시작되지 않은 빈 세션으로 보고 빈 화면을 보여준다.
6. `410 Gone`이면 “대화가 만료되었어요. 새 대화를 시작해주세요.” 안내 후 최신 대화 재조회 또는 새 세션 시작

### 7. Flutter 에러 처리 정책

대표 상태 코드별 UX:

- `404`: 특정 `conversation_id` 복원 실패. 최신 대화 재조회 또는 새 대화로 전환
- `410`: 대화 만료. 새 대화 시작 안내
- `422`: 잘못된 요청. 사용자 입력 검증 확인
- `503`: 챗봇 서버 준비 안 됨. 잠시 후 다시 시도 안내

스트리밍 중 연결 실패 시:

- 현재까지 받은 임시 텍스트는 유지
- “응답이 중간에 끊어졌습니다” 안내
- 사용자가 재시도할 수 있게 버튼 제공

### 8. Flutter 테스트 체크리스트

- 일반 응답에서 `conversation_id`가 정상 저장되는지
- 스트리밍 응답에서 `token`이 순차적으로 이어 붙는지
- `done` 이벤트 이후 `conversation_id`가 갱신되는지
- 앱 재진입 시 대화 조회로 transcript가 복원되는지
- `410 Gone`일 때 새 대화 시작 UX가 맞는지
- assistant 응답이 완료 후 마크다운으로 잘 렌더링되는지

---

## 권장 구현 순서

### 1단계: 백엔드

- Spring Boot JSON relay 먼저 구현
- Spring Boot latest 조회 + conversation 조회 relay 구현
- Spring Boot SSE raw proxy 구현
- 운영 환경 proxy buffering 설정 확인

### 2단계: 프론트엔드

- 일반 응답 기반 채팅 UI 먼저 연결
- `conversation_id` 상태 관리 추가
- latest 조회 + conversation 조회 기반 재진입 복원 추가
- SSE 처리 추가
- 완료 후 markdown 렌더링 추가

---

## 최종 정리

이번 구조의 핵심은 역할 분리다.

백엔드:

- 인증
- `userId` 추출
- FastAPI relay
- SSE raw proxy

프론트엔드:

- `conversation_id` 관리
- 메시지 표시
- SSE 소비
- markdown 렌더링
- 만료/복원 UX 처리

이렇게 분리하면 Flutter는 인증 내부 구조나 FastAPI의 `X-User-Id` 규칙을 몰라도 되고, Spring Boot는 보안과 relay에 집중할 수 있다.
