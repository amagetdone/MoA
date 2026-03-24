# Image Prompt Hardening Design

## Context

The image generation prompt currently parses user input into `[title]` and `[description]` segments before building the final Gemini prompt. The generated result also still allows comic-style overlay artifacts such as speech bubbles because the prompt only suppresses readable text, not non-text overlay shapes.

## Goal

Keep the incoming `user_prompt` intact as the single goal request and harden the system prompt so the model avoids speech bubbles, caption cards, comic panels, sticker-like badges, UI overlays, and similar decorative overlays.

## Chosen Approach

Use a prompt-only change.

- Remove the structured parsing helper and pass the raw `user_prompt` straight into `Goal request`.
- Expand the system prompt with a stricter "overlay suppression" rule covering speech bubbles, thought bubbles, caption boxes, comic frames, stickers, badges, labels, UI overlays, and infographic-like callouts.
- Add tests that lock the new behavior:
  - structured-looking input remains unmodified in `Goal request`
  - the generated prompt contains the new suppression language

## Why This Approach

This directly matches the requested behavior with the smallest code change and the lowest regression risk. It preserves the current image-generation flow, avoids unnecessary preprocessing, and makes the model instruction clearer at the source of generation.

## Risks

- Prompt-only suppression cannot guarantee perfect model compliance, but explicit negative instructions materially reduce these artifacts.
- Existing tests that assumed title/description splitting must be updated.

## Validation

- Run the prompt-focused test module.
- Confirm the structured-input test now expects the raw request.
- Confirm the prompt text contains the new overlay suppression rules.
