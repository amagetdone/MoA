# Image Generation Design

## Context

The AI service already has a new `image_generation` domain under `AI/app/domain`, but its current contract was designed for text-only image generation through an OpenAI-compatible images endpoint.

The new requirement changes the feature into a multimodal image-generation flow:

- the public API must accept `user_prompt`, `image_base64`, and `mime_type`
- the upstream provider must be Google Gemini image generation through the SSAFY GMS relay
- the service should call `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- prompts should still be managed through LangChain and LangGraph
- Langfuse tracing must remain in place without logging raw image payloads

## Decision

Use a hybrid redesign:

- keep the existing `image_generation` domain package
- replace the OpenAI-specific request and response contract with a Gemini multimodal contract
- keep LangGraph as the orchestration layer
- keep LangChain prompt formatting for prompt refinement
- replace the provider call with a small HTTP client boundary that sends Gemini `generateContent` requests through the GMS relay

This avoids creating a duplicate domain while keeping the architecture aligned with the current project direction.

## API Contract

Expose:

- `POST /v1/image-generation`

Request body:

```json
{
  "user_prompt": "Create a futuristic banana with neon lights in a cyberpunk city.",
  "image_base64": "<base64 input image>",
  "mime_type": "image/png"
}
```

Response body:

```json
{
  "image_base64": "<generated image base64>",
  "mime_type": "image/png",
  "model": "gemini-2.5-flash-image",
  "prompt": "<final prompt used upstream>"
}
```

The public API accepts exactly one input image. It does not expose provider-specific options such as candidate count or safety settings.

## GMS Relay Contract

The service will call the GMS relay through:

- `POST {OPENAI_BASE_URL}/models/{IMAGE_GENERATION_MODEL}:generateContent`

Headers:

- `x-goog-api-key: {AI_API_KEY}`
- `Content-Type: application/json`

Payload:

```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "<final prompt from graph>"
        },
        {
          "inline_data": {
            "mime_type": "image/png",
            "data": "<input image base64>"
          }
        }
      ]
    }
  ]
}
```

The implementation should read the generated image from `candidates[].content.parts[].inline_data.data`. If no generated inline image is present, the API should return `502 Bad Gateway` and include safe schema diagnostics for debugging.

## Architecture

Keep the existing domain package under `AI/app/domain/image_generation` and redefine its responsibilities:

- `api/controller/image_generation_controller.py`
  - FastAPI route and request handling
- `api/dto/detail_dto.py`
  - request and response DTOs for multimodal input
- `service/image_generation_service.py`
  - domain service that invokes the graph and calls the GMS Gemini endpoint
- `agents/graph.py`
  - LangGraph `StateGraph` definition
- `agents/nodes/build_prompt_node.py`
  - builds the final Gemini prompt from the user prompt
- `agents/nodes/generate_image_node.py`
  - delegates to the service-layer generator boundary
- `agents/prompts/image_generation_prompt.py`
  - prompt template and helper functions
- `agents/states/state.py`
  - typed graph state for multimodal image generation

The FastAPI app should keep the existing image-generation route path and continue including the router alongside the merchant inference router.

## LangChain and LangGraph Usage

Use LangGraph as the control plane with two nodes:

1. `build_prompt`
   - input: `user_prompt`
   - output: `final_prompt`
   - uses LangChain prompt classes to refine the user request into a concise Gemini image instruction

2. `generate_image`
   - input: `final_prompt`, `input_image_base64`, `input_mime_type`
   - output: `generated_image_base64`, `output_mime_type`, `model`
   - delegates to a generator boundary that builds the Gemini HTTP payload

The graph state should include:

- `user_prompt`
- `input_image_base64`
- `input_mime_type`
- `final_prompt`
- `generated_image_base64`
- `output_mime_type`
- `model`

This keeps the prompt-shaping logic separate from the provider-specific transport.

## Configuration

Keep using `AI/app/core/config.py`, but redefine the image-generation settings for Gemini:

- `ai_api_key`
- `openai_base_url`
  - keep the name for continuity, but point it to the GMS Gemini base path
- `image_generation_model`
  - default: `gemini-2.5-flash-image`
- `langfuse_public_key`
- `langfuse_secret_key`
- `langfuse_base_url`
- `langfuse_tracing_enabled`

The previous `image_generation_size` setting is no longer required for this design and should be removed unless some other feature still depends on it.

Blank strings should still normalize to `None` for optional settings.

## Logging and Tracing

Manage Langfuse integration under `AI/app/core/logging`.

Behavior:

- keep lazy Langfuse initialization
- keep no-op behavior when Langfuse is unconfigured or disabled
- record request-level tracing for the graph execution
- record child-span tracing for the Gemini GMS request
- capture safe metadata such as:
  - `user_prompt`
  - `final_prompt`
  - `input_mime_type`
  - `output_mime_type`
  - `model`
  - safe response schema keys when parsing fails

Do not log:

- API keys
- raw request image base64
- raw generated image base64
- sensitive headers

## Error Handling

Return clear HTTP errors for these cases:

- `422 Unprocessable Entity`
  - blank `user_prompt`
  - blank `image_base64`
  - blank `mime_type`
- `502 Bad Gateway`
  - Gemini response missing a generated inline image
  - malformed upstream response
- `503 Service Unavailable`
  - critical client initialization problems caused by missing required settings

The response body should not leak raw payloads or secrets.

## Testing

Add or update automated coverage for:

- endpoint success path with `user_prompt`, `image_base64`, `mime_type`
- endpoint validation failures for blank fields
- graph behavior with multimodal state
- service payload construction for Gemini `generateContent`
- response parsing from `candidates[].content.parts[]`
- failure diagnostics when inline image data is missing
- Langfuse no-op mode and metadata filtering

Tests should avoid real network calls by stubbing the provider boundary.

## Non-Goals

This task does not include:

- multiple input images
- binary streaming responses
- passing S3 URLs directly to Gemini
- adding provider-specific advanced options to the public API
- migrating the existing merchant inference feature into the new domain structure
