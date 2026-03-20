# Image Generation Async Design

## Context

The current image-generation endpoint keeps the public API contract but executes as a synchronous chain:

- FastAPI route uses `def`
- service entrypoint uses `def generate(...)`
- image download uses `httpx.Client.get(...)`
- Gemini relay call uses `httpx.Client.post(...)`
- LangGraph execution uses synchronous node callables and `invoke(...)`

This means FastAPI can only serve concurrent requests by moving the whole request into a worker thread. The endpoint is not internally non-blocking.

## Goal

Keep the existing `POST /v1/image-generation` request and response schema, but convert the internal execution path to a true async chain so network waits do not block a worker thread.

## Decision

Use a full async conversion for the image-generation flow:

- change the controller to `async def`
- change the service entrypoint to `async def generate(...)`
- use `httpx.AsyncClient` for reference image download and Gemini relay requests
- update the LangGraph generator node to delegate to an async callable
- use graph async execution when available
- keep the API shape, error mapping, and tracing surface unchanged

## Data Flow

1. FastAPI receives `POST /v1/image-generation`
2. controller awaits `service.generate(...)`
3. service downloads the reference image through `AsyncClient.get(...)`
4. service awaits async graph execution
5. graph builds the prompt
6. async generation node awaits the injected image-generator boundary
7. service sends Gemini `generateContent` through `AsyncClient.post(...)`
8. service parses `inline_data` and returns the existing JSON response

## Resource Lifecycle

The image-generation service should own one reusable async HTTP client when created from app startup. The app lifespan should close that client on shutdown so sockets are released cleanly.

Tests may keep injecting fake clients directly without needing real network resources.

## Error Handling

Keep current status mapping:

- `422` for DTO validation errors
- `502` for upstream download or Gemini payload problems
- `503` for configuration or service initialization issues

The async conversion should not change error messages unless required by the new client boundary.

## Testing

Add or update tests to cover:

- controller awaiting an async service fake
- graph async execution with an async generator fake
- service async download and Gemini relay request flow
- missing generated inline data still raising `UpstreamImageGenerationError`
- tracing output still excluding raw base64 payloads

## Non-Goals

This change does not:

- add background job processing
- change the endpoint path or response format
- introduce streaming responses
- widen the public image-generation API
