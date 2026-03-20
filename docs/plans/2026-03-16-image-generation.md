# Image Generation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current text-only OpenAI image-generation flow with a Gemini-based multimodal image-generation endpoint that accepts `user_prompt`, `image_base64`, and `mime_type`, sends a `generateContent` request through the SSAFY GMS relay, and returns generated image base64 in JSON.

**Architecture:** Keep the existing `image_generation` domain, but redefine its DTOs, graph state, service layer, and provider transport around Gemini `generateContent`. LangGraph still orchestrates prompt building and generation, LangChain still formats the prompt, and the provider-specific logic moves to a small HTTP request boundary that uses `x-goog-api-key` headers and parses generated `inline_data` parts from Gemini candidates.

**Tech Stack:** Python 3.12, FastAPI, Pydantic Settings, LangChain, LangGraph, httpx, Langfuse Python SDK, pytest

---

### Task 1: Update configuration tests for the Gemini contract

**Files:**
- Modify: `AI/tests/app/core/test_config_image_generation.py`
- Modify: `AI/pyproject.toml`
- Modify: `AI/.env.example`

**Step 1: Write the failing test**

Update tests so they verify:
- `Settings` still reads `AI_API_KEY` and `OPENAI_BASE_URL`
- `IMAGE_GENERATION_MODEL` defaults to `gemini-2.5-flash-image`
- `IMAGE_GENERATION_SIZE` is no longer required
- blank optional Langfuse values still normalize to `None`

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/core/test_config_image_generation.py -v`
Expected: FAIL because the current settings still reflect the old OpenAI image-generation contract.

**Step 3: Do not change implementation yet**

Stop after the failing test is confirmed.

### Task 2: Implement Gemini-oriented settings and dependency cleanup

**Files:**
- Modify: `AI/app/core/config.py`
- Modify: `AI/pyproject.toml`
- Modify: `AI/.env.example`
- Modify: `AI/tests/app/core/test_config_image_generation.py`

**Step 1: Add minimal implementation**

Update `Settings` to:
- keep `ai_api_key`
- keep `openai_base_url` as the configured GMS Gemini base URL
- default `image_generation_model` to `gemini-2.5-flash-image`
- remove `image_generation_size` if nothing else depends on it
- keep Langfuse settings and blank-string normalization

Remove the OpenAI client dependency if it is no longer used. Keep only the provider libraries actually needed after the redesign.

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/core/test_config_image_generation.py -v`
Expected: PASS

**Step 3: Sync lockfile**

Run: `python -m uv sync --extra dev`
Expected: the environment resolves and `AI/uv.lock` reflects the updated dependency set.

### Task 3: Update Langfuse tests for multimodal metadata filtering

**Files:**
- Modify: `AI/tests/app/core/logging/test_langfuse.py`
- Modify: `AI/app/core/logging/langfuse.py`

**Step 1: Write the failing test**

Update tests so they verify:
- input and output image base64 fields are filtered
- mime types and safe schema metadata are preserved
- no-op tracing still works with the new multimodal field names

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/core/logging/test_langfuse.py -v`
Expected: FAIL because the logging tests still reflect the old payload names.

**Step 3: Stop after the failure**

Do not touch other slices yet.

### Task 4: Implement multimodal-safe Langfuse metadata handling

**Files:**
- Modify: `AI/app/core/logging/langfuse.py`
- Modify: `AI/tests/app/core/logging/test_langfuse.py`

**Step 1: Add minimal implementation**

Update filtering and helper behavior so:
- `image_base64`
- `input_image_base64`
- `generated_image_base64`
- API keys
- authorization-like headers

are always removed from Langfuse metadata and output.

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/core/logging/test_langfuse.py -v`
Expected: PASS

**Step 3: Refactor lightly**

Keep the tracing API surface small and provider-agnostic.

### Task 5: Rewrite prompt and graph tests for multimodal state

**Files:**
- Modify: `AI/tests/app/domain/image_generation/test_prompt_graph.py`
- Modify: `AI/app/domain/image_generation/agents/prompts/image_generation_prompt.py`
- Modify: `AI/app/domain/image_generation/agents/states/state.py`
- Modify: `AI/app/domain/image_generation/agents/graph.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/build_prompt_node.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/generate_image_node.py`

**Step 1: Write the failing test**

Update tests so they verify:
- `user_prompt` becomes a refined `final_prompt`
- the graph carries `input_image_base64` and `input_mime_type`
- the graph returns `generated_image_base64`, `output_mime_type`, and `model`
- generation still happens through an injected callable instead of inline HTTP logic

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_prompt_graph.py -v`
Expected: FAIL because the current graph still uses the old text-only state contract.

**Step 3: Stop after the failure**

Keep the scope isolated to the graph slice.

### Task 6: Implement the multimodal LangGraph flow

**Files:**
- Modify: `AI/app/domain/image_generation/agents/prompts/image_generation_prompt.py`
- Modify: `AI/app/domain/image_generation/agents/states/state.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/build_prompt_node.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/generate_image_node.py`
- Modify: `AI/app/domain/image_generation/agents/graph.py`
- Modify: `AI/tests/app/domain/image_generation/test_prompt_graph.py`

**Step 1: Add minimal implementation**

Implement:
- a state with `user_prompt`, `input_image_base64`, `input_mime_type`, `final_prompt`, `generated_image_base64`, `output_mime_type`, `model`
- a prompt builder tailored to Gemini image editing or transformation instructions
- a generation node that passes prompt and image input through the injected generator boundary
- a graph wiring `build_prompt -> generate_image`

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_prompt_graph.py -v`
Expected: PASS

**Step 3: Refactor lightly**

Keep prompt wording centralized in the prompt module.

### Task 7: Rewrite service tests for Gemini `generateContent`

**Files:**
- Modify: `AI/tests/app/domain/image_generation/test_image_generation_service.py`
- Modify: `AI/app/domain/image_generation/service/image_generation_service.py`

**Step 1: Write the failing test**

Update service tests so they verify:
- the service builds a Gemini request body with `contents[0].parts[text, inline_data]`
- the service uses `x-goog-api-key` authentication instead of OpenAI auth
- the service parses generated image data from `candidates[].content.parts[].inline_data.data`
- the service raises a domain error when no generated inline image exists
- trace metadata records safe schema keys without logging raw image payloads

Use a fake HTTP client boundary rather than real network calls.

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: FAIL because the current service still uses the OpenAI images API contract.

**Step 3: Stop after the failure**

Do not modify the API layer yet.

### Task 8: Implement Gemini service transport and response parsing

**Files:**
- Modify: `AI/app/domain/image_generation/service/image_generation_service.py`
- Modify: `AI/app/domain/image_generation/agents/nodes/generate_image_node.py`
- Modify: `AI/tests/app/domain/image_generation/test_image_generation_service.py`

**Step 1: Add minimal implementation**

Implement:
- a service entrypoint that accepts `user_prompt`, `image_base64`, `mime_type`
- a small HTTP client boundary using `httpx` or an equivalent injected transport
- `POST {OPENAI_BASE_URL}/models/{IMAGE_GENERATION_MODEL}:generateContent`
- headers with `x-goog-api-key`
- a request body using `text` and `inline_data`
- parsing logic that scans `candidates[].content.parts[]` for generated `inline_data.data`
- safe schema diagnostics when parsing fails

Wrap:
- the overall request in a Langfuse trace
- the GMS Gemini request in a child span

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: PASS

**Step 3: Refactor lightly**

Keep Gemini transport details inside the service boundary.

### Task 9: Rewrite API contract tests for multimodal input

**Files:**
- Modify: `AI/tests/app/test_image_generation_api.py`
- Modify: `AI/app/domain/image_generation/api/dto/detail_dto.py`
- Modify: `AI/app/domain/image_generation/api/controller/image_generation_controller.py`
- Modify: `AI/app/main.py`
- Modify: `AI/app/api/dependencies.py`

**Step 1: Write the failing test**

Update FastAPI tests so they verify:
- `POST /v1/image-generation` accepts `user_prompt`, `image_base64`, `mime_type`
- success returns `image_base64`, `mime_type`, `model`, and `prompt`
- blank fields return `422`
- missing generated inline image becomes `502`

Use app-state injection with a fake multimodal image-generation service.

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/test_image_generation_api.py -v`
Expected: FAIL because the current DTOs and controller still use `prompt_goal`.

**Step 3: Stop after the failure**

Do not widen scope beyond the API contract yet.

### Task 10: Implement DTO and controller rewiring

**Files:**
- Modify: `AI/app/domain/image_generation/api/dto/detail_dto.py`
- Modify: `AI/app/domain/image_generation/api/controller/image_generation_controller.py`
- Modify: `AI/app/main.py`
- Modify: `AI/app/api/dependencies.py`
- Modify: `AI/tests/app/test_image_generation_api.py`

**Step 1: Add minimal implementation**

Implement:
- request DTO fields `user_prompt`, `image_base64`, `mime_type`
- response DTO fields `image_base64`, `mime_type`, `model`, `prompt`
- controller logic that passes the new inputs to the service
- domain exception mapping for malformed Gemini responses
- app startup wiring that still initializes the image-generation service from settings

**Step 2: Run focused tests**

Run: `python -m uv run pytest tests/app/test_image_generation_api.py -v`
Expected: PASS

**Step 3: Run the updated feature suite**

Run: `python -m uv run pytest tests/app/core/test_config_image_generation.py tests/app/core/logging/test_langfuse.py tests/app/domain/image_generation/test_prompt_graph.py tests/app/domain/image_generation/test_image_generation_service.py tests/app/test_image_generation_api.py -v`
Expected: PASS

### Task 11: Verify integration and review the diff

**Files:**
- Verify: `AI/app/core/config.py`
- Verify: `AI/app/core/logging/langfuse.py`
- Verify: `AI/app/domain/image_generation/...`
- Verify: `AI/app/main.py`
- Verify: `AI/pyproject.toml`
- Verify: `AI/.env.example`

**Step 1: Run a broader regression slice**

Run: `python -m uv run pytest -v`
Expected: PASS, or only known unrelated failures already present before this work.

**Step 2: Run a lightweight import smoke test**

Run: `python -m uv run python -c "from app.main import create_app; app = create_app(classifier=object(), settings_override={'AI_API_KEY':'test-key','OPENAI_BASE_URL':'https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta'}); print(app.url_path_for('health'))"`
Expected: the app initializes without attempting a real Gemini generation request.

**Step 3: Review git diff**

Run: `git diff -- AI/app/main.py AI/app/api/dependencies.py AI/app/core/config.py AI/app/core/logging AI/app/domain/image_generation AI/tests/app AI/pyproject.toml AI/.env.example AI/uv.lock`
Expected: only the planned files and dependency updates appear.

**Step 4: Commit**

If the full verification passes, create a focused commit for the implementation work.
