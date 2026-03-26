# MOA Chatbot Conversation Memory Design

## Context

The chatbot currently supports:

- `POST /v1/chatbot/messages`
- `POST /v1/chatbot/messages/stream`
- LangGraph multi-turn memory through `InMemorySaver`

That is enough for same-process conversational context, but it is not enough for frontend session restoration. There is no API to read conversation history by `conversation_id`, and there is no explicit expiration policy for user-visible chat history.

The new requirement is:

- keep conversation sessions valid for 3 days from the last message
- allow the frontend to fetch conversation history by `conversation_id`
- do not introduce PostgreSQL persistence for this feature

Because storage must remain in memory only, cross-restart durability is explicitly out of scope.

## Goals

- Add a user-scoped in-memory conversation store for UI-visible chat history.
- Apply a sliding 3-day TTL based on the most recent activity.
- Add a history retrieval endpoint keyed by `conversation_id`.
- Preserve the existing `LangGraph` multi-turn behavior.
- Prevent cross-user access to conversation history.

## Non-Goals

- No PostgreSQL tables, migrations, or durable storage.
- No conversation list endpoint.
- No recovery of conversations after server restart or process replacement.
- No storage of internal specialist messages or stream token chunks.

## Chosen Approach

Use a dedicated in-memory `ConversationMemoryStore` alongside the existing LangGraph `InMemorySaver`.

The two memory layers serve different purposes:

- `InMemorySaver`: preserves LangGraph execution state for follow-up turns
- `ConversationMemoryStore`: preserves frontend-visible transcript metadata and enforces the 3-day TTL

This separation keeps the existing graph behavior intact while making transcript lookup and expiration explicit and easy to test.

## Data Model

The store will keep entries keyed by `(user_id, conversation_id)`.

Each conversation will contain:

- `conversation_id`
- `user_id`
- `created_at`
- `last_activity_at`
- `expires_at`
- `messages`

Each stored message will contain:

- `role` (`user` or `assistant`)
- `content`
- `created_at`

The store will expose operations for:

- creating or loading an active conversation
- appending a user message
- appending an assistant message
- retrieving an active conversation
- rejecting expired conversations
- opportunistically pruning expired entries

## TTL Policy

TTL is a sliding 3-day window.

- When a new conversation starts, `expires_at = now + 3 days`
- When the user sends a new message to an active conversation, `last_activity_at` and `expires_at` are refreshed
- When the assistant reply completes, the same conversation remains active with the refreshed expiration
- When `now > expires_at`, the conversation is considered expired

Expired conversations will:

- return `410 Gone` on history lookup
- return `410 Gone` when the client tries to continue the same `conversation_id`
- be removed from the in-memory store during access or periodic pruning opportunities

This prevents stale LangGraph thread state from being silently reused after expiration.

## API Changes

### Existing endpoints

`POST /v1/chatbot/messages`

- Start a new conversation when `conversation_id` is absent
- Continue an active conversation when `conversation_id` is provided
- Return `410 Gone` if the provided conversation is expired

`POST /v1/chatbot/messages/stream`

- Same conversation rules as the JSON endpoint
- Persist the final assistant answer only after streaming completes
- Do not persist stream token chunks

### New endpoint

`GET /v1/chatbot/conversations/{conversation_id}`

Required header:

`X-User-Id`

Response shape:

```json
{
  "conversation_id": "conv-1",
  "created_at": "2026-03-26T10:00:00+09:00",
  "last_activity_at": "2026-03-26T10:05:00+09:00",
  "expires_at": "2026-03-29T10:05:00+09:00",
  "messages": [
    {
      "role": "user",
      "content": "이번 주 예산 어때?",
      "created_at": "2026-03-26T10:00:00+09:00"
    },
    {
      "role": "assistant",
      "content": "이번 주 자유 예산은 아직 여유가 있습니다.",
      "created_at": "2026-03-26T10:00:02+09:00"
    }
  ]
}
```

Error behavior:

- `404 Not Found`: conversation id was never seen for that user
- `410 Gone`: conversation existed but expired
- `422`: invalid `X-User-Id` or blank path value

## User Isolation

Conversation access will always be scoped by:

- trusted upstream `X-User-Id`
- requested `conversation_id`

The store must never allow lookup by `conversation_id` alone. The same `conversation_id` string may exist under different users, but they are isolated by composite key.

## Recording Rules

Only user-visible messages will be stored:

- user request text
- final assistant response text

The following will not be stored:

- intermediate stream chunks
- supervisor routing status events
- internal specialist messages
- tool call payloads

This keeps the transcript small, readable, and safe for direct frontend display.

## Failure Handling

For non-streaming replies:

- append the user message before invoking the graph
- append the assistant message only after a successful final answer is produced

For streaming replies:

- append the user message before streaming starts
- accumulate visible assistant tokens in memory
- append one assistant message after the stream completes
- if generation fails before completion, keep only the user message for that turn

This makes failures visible without fabricating assistant output.

## Testing Strategy

Add tests for:

- in-memory store create, append, fetch, prune behavior
- TTL extension on each new message
- expired conversation lookup returning `410`
- expired conversation continuation returning `410`
- per-user isolation for the same `conversation_id`
- message endpoint and stream endpoint recording transcripts
- conversation history endpoint contract

## Trade-Offs

Advantages:

- simple implementation
- no new infrastructure
- explicit TTL behavior
- easy frontend integration through a single lookup endpoint

Costs:

- conversation history disappears on server restart
- no multi-instance sharing
- transcript memory grows until entries expire or are pruned

These trade-offs are acceptable for the approved in-memory-only scope.
