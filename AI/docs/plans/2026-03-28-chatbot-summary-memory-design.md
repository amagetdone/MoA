# Chatbot Summary Memory Design

**Goal:** Add `ConversationSummaryMemory`-based internal conversation summarization to the chatbot while preserving the current external API contract and conversation ID flow.

## Constraints

- Keep `conversation_id` issuance and reuse exactly as-is.
- Keep `/v1/chatbot/messages`, `/v1/chatbot/messages/stream`, `/v1/chatbot/conversations/latest`, and `/v1/chatbot/conversations/{conversation_id}` response contracts unchanged.
- Keep raw conversation messages available for history lookup.
- Limit summarization input to user messages and the assistant's final answers only.
- Minimize structural change to the current repository and service layers.

## Recommended Approach

Use the existing `ConversationMemoryStore` as the source of truth for TTL and raw transcript storage, and add a small internal summary state alongside it.

- `ConversationMemoryStore` remains responsible for:
  - raw messages
  - `conversation_id`
  - `created_at`, `last_activity_at`, `expires_at`
  - TTL expiration and deletion
- A small summarizer adapter wraps `ConversationSummaryMemory`.
- `ChatbotService` uses the adapter only for prompt context compaction.

## Internal Data Model Change

Add two internal-only fields to each conversation record:

- `summary_text`: accumulated summary of already summarized turns
- `summarized_message_count`: how many leading raw messages are already covered by `summary_text`

This allows the service to build prompt context as:

- one synthetic summary message for already summarized turns
- the unsummarized raw tail only

This keeps the external history API unchanged because raw messages are still stored in full.

## Service Flow

### Reply

1. Append the user message to the raw store.
2. Build graph input from:
   - summary message, if present
   - unsummarized raw tail
3. Run the agent.
4. Append the assistant final answer to the raw store.
5. Summarize the newly completed unsummarized tail with `ConversationSummaryMemory`.
6. Persist the updated summary state in the store.

### Stream

1. Append the user message to the raw store.
2. Build graph input from summary + unsummarized raw tail.
3. Stream the assistant final output as today.
4. After stream completion, append the final assistant answer.
5. Summarize the unsummarized tail and persist the updated summary state.

## Why This Is Minimal Change

- No controller or DTO changes.
- No change to conversation lookup endpoints.
- No change to `conversation_id` behavior.
- TTL stays entirely inside `ConversationMemoryStore`.
- Existing direct-service tests can keep the old behavior unless a summarizer is explicitly injected.

## Dependency Note

The current environment does not expose `ConversationSummaryMemory` from `langchain.memory`. The available compatible implementation is `langchain_classic.memory.ConversationSummaryMemory`, which requires adding the `langchain-classic` package.

## Verification

- Add tests for the repository summary state while keeping external conversation payloads unchanged.
- Add service tests proving summary mode compacts graph input without changing the external response contract.
- Run chatbot regression tests and API tests after the change.
