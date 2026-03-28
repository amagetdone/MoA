# Chatbot Graph Simplification Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 소비 패턴 코칭 챗봇 graph를 한 단계 단순화해 nested supervisor 없이도 SQL 승인 loop, 코칭 생성, fallback 응답 보장을 안정적으로 유지한다.

**Architecture:** top-level 및 team-level supervisor를 제거하고 `QuerySmith -> QueryGuard` 조건부 retry loop와 `SQL executor -> SpendWise Coach -> deterministic finalizer` 흐름으로 재구성한다. 최종 응답은 LLM handoff tool이 아니라 graph finalizer가 직접 채운다.

**Tech Stack:** Python, LangGraph StateGraph, LangChain agents, pytest

---

### Task 1: 단순화된 graph 기대 동작을 테스트로 고정

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- Modify: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Write the failing tests**

- SQL 3회 reject 시 `final_response`가 safe fallback markdown으로 채워지는 테스트를 추가한다.
- SQL 승인 후 coach draft가 있으면 최종 응답으로 사용되는 테스트를 추가한다.
- coach draft가 비어 있으면 fallback으로 대체되는 테스트를 추가한다.
- 서비스 레벨에서 `answer == ""` 회귀를 막는 테스트를 추가한다.

**Step 2: Run tests to verify they fail**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: 새 테스트가 fail 하거나 현재 supervisor 구조 가정과 충돌한다.

### Task 2: graph 구조 단순화

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/agents/states/state.py`

**Step 1: Remove nested supervisors**

- top-level `create_supervisor()`를 제거한다.
- SQL team / Coaching team 내부 supervisor 구성도 제거한다.

**Step 2: Build a direct StateGraph flow**

- `QuerySmith -> QueryGuard` retry loop를 conditional edge로 직접 구성한다.
- 승인되면 `SQL executor -> SpendWise Coach -> finalize_response`로 연결한다.
- 3회 reject면 `finalize_response`로 바로 연결한다.

**Step 3: Add deterministic final response assembly**

- `finalize_response` node를 추가해 `final_response`와 `final_answer`를 직접 채운다.
- fallback markdown도 여기서 보장한다.

### Task 3: dead code 및 prompt 의존 경로 정리

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Remove obsolete forwarding path**

- `forward_final_response` tool과 관련 helper가 더 이상 필요 없으면 제거한다.
- top-level supervisor routing prompt 의존성을 제거한다.

**Step 2: Keep only prompts still needed**

- QuerySmith, QueryGuard, SpendWise Coach prompt는 유지한다.
- 더 이상 쓰이지 않는 orchestration prompt는 제거하거나 최소화한다.

### Task 4: Verify and document

**Files:**
- Modify: `docs/plans/2026-03-27-chatbot-graph-simplification-design.md`

**Step 1: Run focused tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: PASS

**Step 2: Run chatbot domain regression**

Run: `uv run python -m pytest tests/app/domain/chatbot -q`

Expected: PASS

**Step 3: Summarize final behavior changes**

- nested supervisor 제거
- final_response deterministic 보장
- empty answer 회귀 차단
