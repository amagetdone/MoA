# Image Generation Async Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Convert the existing image-generation endpoint to a true async execution chain while keeping the same public API contract.

**Architecture:** Keep the FastAPI route, DTOs, and Gemini payload contract intact, but convert controller, service, graph execution, and HTTP boundaries to `async/await`. Reuse one `httpx.AsyncClient` per service instance and close it in app lifespan cleanup.

**Tech Stack:** Python 3.12, FastAPI, httpx, LangGraph, pytest

---

### Task 1: Add failing async graph and service tests

**Files:**
- Modify: `AI/tests/app/domain/image_generation/test_prompt_graph.py`
- Modify: `AI/tests/app/domain/image_generation/test_image_generation_service.py`

**Step 1: Write the failing tests**

Add tests that verify:
- the graph can be awaited through `ainvoke(...)`
- the injected generator can be an async callable
- the service entrypoint is awaited
- fake HTTP client methods are awaited instead of called synchronously

**Step 2: Run tests to verify they fail**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_prompt_graph.py tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: FAIL because the current implementation is synchronous.

### Task 2: Add failing async API test

**Files:**
- Modify: `AI/tests/app/test_image_generation_api.py`

**Step 1: Write the failing test**

Update the fake service to expose `async def generate(...)` and verify the route still returns the same payload.

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/test_image_generation_api.py -v`
Expected: FAIL because the controller currently returns an un-awaited coroutine.

### Task 3: Implement async graph and service flow

**Files:**
- Modify: `AI/app/domain/image_generation/agents/graph.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/generate_image_node.py`
- Modify: `AI/app/domain/image_generation/service/image_generation_service.py`

**Step 1: Add minimal implementation**

Implement:
- async image generator callable typing
- async generation node
- service `async def generate(...)`
- async reference image download
- async Gemini relay request
- async graph execution

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_prompt_graph.py tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: PASS

### Task 4: Implement async controller and app lifecycle cleanup

**Files:**
- Modify: `AI/app/domain/image_generation/api/controller/image_generation_controller.py`
- Modify: `AI/app/main.py`
- Modify: `AI/tests/app/test_image_generation_api.py`

**Step 1: Add minimal implementation**

Implement:
- async controller awaiting the service
- app lifespan cleanup that closes the image-generation service client when present

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/test_image_generation_api.py -v`
Expected: PASS

### Task 5: Verify the image-generation slice

**Files:**
- Verify: `AI/app/domain/image_generation/...`
- Verify: `AI/app/main.py`
- Verify: `AI/tests/app/domain/image_generation/...`
- Verify: `AI/tests/app/test_image_generation_api.py`

**Step 1: Run the async image-generation suite**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_prompt_graph.py tests/app/domain/image_generation/test_image_generation_service.py tests/app/test_image_generation_api.py -v`
Expected: PASS

**Step 2: Review the diff**

Run: `git diff -- AI/app/main.py AI/app/domain/image_generation AI/tests/app/domain/image_generation AI/tests/app/test_image_generation_api.py docs/plans/2026-03-20-image-generation-async-design.md docs/plans/2026-03-20-image-generation-async.md`
Expected: only the async image-generation changes appear.
