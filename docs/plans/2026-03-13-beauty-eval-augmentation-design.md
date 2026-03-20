# Beauty Eval Augmentation Design

## Context

The current merchant-info augmentation appends all weakly labeled rows to train only. After the beauty-retail relabeling, the official valid/test splits no longer contain any `미용` rows, which makes the class unobservable during evaluation.

## Decision

Keep the existing train-heavy augmentation strategy for most categories, but split merchant-info `미용` rows across train/valid/test and merge them directly into the official evaluation splits.

## Scope

- Continue adding non-beauty merchant-info rows to train only
- Add merchant-info `미용` rows to train/valid/test with an 8:1:1 split
- Keep the rest of the notebook flow unchanged

## Constraints

- Do not duplicate rows already present in any base split
- Preserve deterministic split behavior through the existing seed
- Make the new behavior opt-in, so the augmentation module still supports train-only mode by default

## Verification

- Regression test for existing train-only default behavior
- Regression test for category-targeted eval augmentation
- Notebook source updated to pass `미용` eval augmentation settings
