# Register Merchants Daegu Gyeongbuk Design

## Context

`lab/register_merchants.ipynb` currently discovers every regional merchant CSV under `lab/merchant_info` and loads all merchant names. The source files are already split by region, so row-level filtering is unnecessary for the requested scope.

## Goal

Limit notebook CSV loading to only the Daegu and Gyeongbuk regional files.

## Chosen Approach

Filter discovered CSV paths in the notebook by filename.

- Keep using `discover_merchant_csv_paths(INPUT_PATH)`.
- Add a small filename filter for `_대구_` and `_경북_`.
- Keep the downstream `load_merchant_names(csv_paths)` call unchanged.
- Clear stale notebook outputs in the modified preview cell so the saved notebook does not still show all 17 CSVs.

## Why This Approach

The input data is already region-partitioned by filename, so filtering earlier in the notebook is the simplest and lowest-risk solution. It avoids changing reusable loader utilities that may be used elsewhere.

## Validation

- Verify the notebook source now includes the Daegu/Gyeongbuk filename filter.
- Verify the preview cell output is cleared so it no longer shows the old all-region counts.
