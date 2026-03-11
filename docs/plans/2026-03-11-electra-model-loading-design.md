# ELECTRA Model Loading Design

## Summary

Keep `AutoModelForSequenceClassification` in the API loader so the Hugging Face model repo remains the source of truth for architecture selection. Add a small runtime validation step so startup fails fast if the uploaded model no longer resolves to an ELECTRA sequence-classification model.

## Decision

We will not hardcode `ElectraForSequenceClassification.from_pretrained(...)`.

We will:
- keep the existing auto-model loading path in `AI/app/core/model_loader.py`
- validate the resolved model class and config metadata after loading
- add regression tests that prove ELECTRA metadata is accepted and non-ELECTRA metadata is rejected

## Why This Approach

This keeps the loader flexible for the existing repository while still protecting us from broken or stale Hugging Face metadata. If the model repo accidentally ships a mismatched architecture later, the service will fail during startup instead of serving incorrect predictions.

## Error Handling

If the resolved model is not an ELECTRA sequence classifier, the loader should raise a clear `ValueError` that includes the model source and the unexpected class/config details.

## Testing

Use TDD:
- add a failing unit test for the accepted ELECTRA path
- add a failing unit test for the rejected non-ELECTRA path
- implement the minimal runtime validation in the loader
- run the targeted unit tests
