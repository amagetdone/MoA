# MOA Chatbot Markdown Hardening Design

## Context

The chatbot already asks the supervisor to respond in basic markdown, but the instruction is too soft. That makes the final answer drift back into plain-text blocks and weak structure.

There is also a tool-alignment issue in the anomaly path. Budget and spending tools resolve relative windows from the chatbot timezone-aware `today_factory`, while the anomaly repository still uses `date.today()` internally. This can produce inconsistent date windows across specialists.

## Chosen Approach

Keep the change prompt-first, but make the response contract explicit and harder to ignore.

- strengthen only the supervisor prompt because only supervisor output is user-facing
- require markdown structure, not just optional markdown
- forbid plain-text walls when list or section structure would clarify the answer
- keep specialist tools unchanged except for anomaly date resolution consistency

## Behavior

The supervisor should:

- always return valid, readable markdown
- prefer a short heading plus concise sections when the answer contains multiple ideas
- use flat bullet lists for action items or grouped facts
- use a table when comparing numeric categories or period-over-period values
- avoid decorative formatting and deep nesting

The anomaly tool should:

- resolve relative windows from the same injected date source as the other read-only tools
- respect the configured chatbot timezone indirectly through `today_factory`

## Non-Goals

- no markdown post-processing layer
- no API schema changes
- no broad tool redesign into free-form SQL or generic query planning

## Validation

- add stricter prompt-level assertions for markdown structure guidance
- add a regression test proving the anomaly tool uses the injected date source
- run targeted chatbot prompt, tool, service, and graph tests
