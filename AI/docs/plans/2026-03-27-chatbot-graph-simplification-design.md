# Chatbot Graph Simplification Design

## Goal

소비 패턴 코칭 챗봇의 내부 graph를 한 단계 단순화해, 현재 기능 범위에 비해 과한 nested supervisor 구조를 제거하고 fallback/최종 응답 보장을 더 안정적으로 만든다.

## Current Problem

- 현재 graph는 top-level supervisor, SQL team supervisor, Coaching team supervisor까지 중첩돼 있다.
- 이 구조는 SQL 승인 실패나 코칭 fallback 자체보다, "최종 응답을 누가 언제 전달하느냐"가 더 취약점이 되고 있다.
- 실제로 `warnings`와 `tool_summaries`는 남는데 `answer`가 빈 문자열이 되는 경우가 발생했다.
- 소비 패턴 코칭 하나를 처리하기에는 orchestration 복잡도가 과하다.

## Chosen Direction

현재 유스케이스에 맞춰 graph를 아래처럼 단순화한다.

1. top-level `create_supervisor()` 제거
2. SQL team / Coaching team 내부 `create_supervisor()` 제거
3. `QuerySmith -> QueryGuard`는 조건부 loop로 유지
4. SQL 승인 시 `SQL executor -> SpendWise Coach`
5. 최종 응답은 LLM supervisor tool call이 아니라 deterministic finalizer가 채운다

## Target Architecture

```text
START
  -> prepare_querysmith_input
  -> querysmith
  -> capture_querysmith_result
  -> prepare_queryguard_input
  -> queryguard
  -> capture_queryguard_result
  -> route_sql_validation
       - approved -> prepare_sql_executor_input -> sql_executor -> capture_sql_result
                   -> prepare_spendwise_coach_input -> spendwise_coach -> capture_coach_draft
                   -> finalize_response
       - retry    -> querysmith
       - fallback -> finalize_response
END
```

## What We Keep

- `QuerySmith` / `QueryGuard` 분리
- 구조화 remediation 기반 SQL 재시도
- deterministic SQL guardrail
- `SpendWise Coach`를 통한 코칭 markdown 생성
- `warnings`, `tool_summaries`, `failure_reason` state 축적

## What We Remove

- top-level supervisor의 `forward_final_response` tool path
- SQL team 내부 supervisor handoff
- Coaching team 내부 supervisor handoff
- `ResponseGuard` 기반 별도 코칭 검증 loop

## Response Handling

- SQL 승인 실패 시 deterministic safe fallback markdown을 즉시 최종 응답으로 채운다.
- SQL 실행 및 코칭 작성까지 성공하면 coach draft를 최종 응답으로 채운다.
- coach draft가 비어 있거나 이상하면 deterministic fallback markdown으로 대체한다.
- 즉, `final_response`는 graph 종료 시 항상 비어 있지 않도록 한다.

## Risks

- `ResponseGuard` 제거로 코칭 품질 검증 한 겹이 사라진다.
- 대신 현재 범위에서는 "빈 응답 방지"와 "흐름 단순화"의 이점이 더 크다.
- 필요하면 이후 `SpendWise Coach` 뒤에 deterministic markdown sanity check 정도만 다시 얹을 수 있다.

## Testing Strategy

- SQL 3회 reject 시 `final_response`가 반드시 safe fallback으로 채워지는지 테스트
- SQL 승인 후 coach draft가 있으면 그 값이 최종 응답이 되는지 테스트
- coach draft가 비어 있으면 deterministic fallback으로 내려가는지 테스트
- `answer`가 빈 문자열이 되는 회귀를 서비스 레벨에서 막는 테스트 추가
