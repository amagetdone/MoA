# Image Generation Design

## Context

The current AI service already exposes FastAPI endpoints for merchant inference under `AI/app`, but it does not yet follow the fuller `app/domain/<domain>` structure described in `AI/AGENTS.md`.

This feature adds an image generation endpoint that must not call OpenAI directly. Instead, it must use the SSAFY GMS relay server configured through environment variables. The user also wants the implementation to stay consistent with future agent-style features by using LangChain and LangGraph where practical, with prompts managed in a separate Python module.

## Decision

Use a hybrid approach:

- Keep the existing merchant inference code unchanged.
- Add the new feature under a fresh `image_generation` domain structure.
- Use LangGraph for workflow orchestration.
- Use LangChain prompt packages for prompt management and formatting.
- Use the OpenAI Python SDK only for the final image generation call so the request is guaranteed to go through `OPENAI_BASE_URL/v1/images/generations`.

This keeps the new code aligned with the project direction without forcing a risky migration of the existing API structure.

## API Contract

Expose a new endpoint:

- `POST /v1/image-generation`

Request body:

```json
{
  "prompt_goal": "우주 배경의 초현실적인 고양이 일러스트"
}
```

Response body:

```json
{
  "image_base64": "<base64 image payload>",
  "mime_type": "image/png",
  "model": "dall-e-3",
  "prompt": "A surreal illustration of a cat set against the backdrop of space."
}
```

The public API only accepts `prompt_goal`. The actual image model and size are fixed by server configuration and are not exposed as request fields.

## GMS Relay Contract

The service will call the GMS relay through:

- `POST {OPENAI_BASE_URL}/images/generations`

Headers:

- `Authorization: Bearer {AI_API_KEY}`
- `Content-Type: application/json`

Payload:

```json
{
  "model": "dall-e-3",
  "prompt": "<final prompt from graph>",
  "size": "1024x1024"
}
```

The implementation will expect the OpenAI-compatible image response shape and read the base64 image from `data[0].b64_json`. If the relay returns a malformed response or omits `b64_json`, the API should return `502 Bad Gateway`.

## Architecture

Create a new domain package under `AI/app/domain/image_generation` with these responsibilities:

- `api/controller/image_generation_controller.py`
  - FastAPI route and request handling
- `api/dto/detail_dto.py`
  - Request and response DTOs
- `service/image_generation_service.py`
  - Domain service that invokes the graph and maps errors
- `agents/graph.py`
  - LangGraph `StateGraph` definition
- `agents/nodes/build_prompt_node.py`
  - Builds the final prompt from the user goal
- `agents/nodes/generate_image_node.py`
  - Calls the OpenAI SDK against the GMS relay
- `agents/prompts/image_generation_prompt.py`
  - Prompt template and helper functions
- `agents/states/state.py`
  - Typed graph state

The FastAPI app should include this new router alongside the existing inference router.

## LangChain and LangGraph Usage

Use LangGraph as the control plane for the feature. The graph should have two simple nodes:

1. `build_prompt`
   - Input: `prompt_goal`
   - Output: `final_prompt`
   - Uses LangChain prompt classes to build a generation-ready image prompt from the user goal.

2. `generate_image`
   - Input: `final_prompt`
   - Output: `image_base64`, `mime_type`, `model`
   - Uses the OpenAI SDK configured with `base_url=OPENAI_BASE_URL`.

This keeps orchestration, prompt management, and future extensibility aligned with the agent-oriented structure the project is moving toward.

## Configuration

Extend `AI/app/core/config.py` with image-generation and tracing settings:

- `ai_api_key`
- `openai_base_url`
- `image_generation_model` with default `dall-e-3`
- `image_generation_size` with default `1024x1024`
- `langfuse_public_key`
- `langfuse_secret_key`
- `langfuse_base_url`
- `langfuse_tracing_enabled`

Blank strings should be normalized to `None` for optional settings.

The service should document these values in `.env.example` if that file exists or create it if missing.

## Logging and Tracing

Manage Langfuse integration under `AI/app/core/logging`.

Recommended files:

- `AI/app/core/logging/__init__.py`
- `AI/app/core/logging/langfuse.py`

Behavior:

- Initialize Langfuse lazily from settings.
- If Langfuse credentials are missing, tracing becomes a no-op without breaking the endpoint.
- Record request-level tracing for the graph execution.
- Capture metadata such as:
  - `prompt_goal`
  - `final_prompt`
  - `model`
  - `size`
  - success or failure
  - upstream error message when available

Do not log:

- API keys
- raw image base64 payloads
- sensitive headers

The LangGraph flow and the upstream GMS image generation call should each be wrapped in their own trace or span so failures can be distinguished quickly.

## Error Handling

Return clear HTTP errors for these cases:

- `422 Unprocessable Entity`
  - invalid request body, such as blank `prompt_goal`
- `502 Bad Gateway`
  - GMS/OpenAI-compatible response missing `b64_json`
  - malformed upstream response
- `503 Service Unavailable`
  - critical client initialization problems caused by missing required settings

The response body should not leak secrets or raw upstream headers.

## Testing

Add automated coverage for:

- endpoint success path
- endpoint failure when upstream response does not contain `b64_json`
- prompt-building behavior
- graph execution result mapping
- Langfuse no-op mode when tracing is disabled or unconfigured
- correct upstream payload shape containing only configured `model`, computed `prompt`, and configured `size`

Tests should avoid real network calls by stubbing the OpenAI client boundary.

## Non-Goals

This task does not include:

- streaming binary image responses
- exposing model, size, quality, or style as public request fields
- migrating the existing merchant inference feature into the new domain structure
