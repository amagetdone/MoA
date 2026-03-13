# Account OpenAPI Design

## Goal

Implement the account domain around SSAFY demand-deposit OpenAPI so the backend clearly exposes:

- management APIs for demo preparation
- user APIs for account linkage and account operations
- local account persistence synchronized from OpenAPI account list results

## Context

- The current `domain/account` package is mostly a path-only move from `domain/asset`.
- Source files inside `domain/account` still declare `domain.asset` packages and class names.
- The Flutter app does not create accounts directly. Users only link or use existing accounts.
- The backend still needs account/product creation for demo and admin workflows.

## Domain Boundary

### Management APIs

- demand-deposit product registration
- demand-deposit product list
- demand-deposit account creation

These endpoints exist so demo data can be prepared before FE account linkage.

### User APIs

- sync my accounts from OpenAPI into local DB
- list my linked accounts from local DB
- inquire account balance
- deposit into account
- transfer between accounts
- list transaction history

## Architecture

### Controller layer

Split account endpoints into two clear groups:

- `AccountAdminController`
- `AccountController`

Swagger should clearly show the distinction between admin/demo APIs and user APIs.

### Application layer

Use focused use cases for each business action:

- register product
- list products
- create account
- sync account list
- list local accounts
- inquire balance
- deposit
- transfer
- list transactions

`AccountService` can orchestrate user-facing calls, but each business action remains isolated and testable.

### Infra layer

Add SSAFY demand-deposit clients under `domain/account/infra/client`.

The implementation should reuse:

- `RestClient`
- `SsafyApiRequestFactory`
- `SsafyApiResponseValidator`

The existing transaction client for demand-deposit history should be reused instead of duplicated where practical.

## Persistence Strategy

Local persistence is needed only for linked account metadata that our service owns.

Persist these account-level fields from OpenAPI account-list/account-detail responses:

- local account id
- user reference
- account number
- account type unique number if known
- bank code
- bank name
- alias
- verified flag

Local account sync should:

1. fetch the authenticated user's account list from OpenAPI
2. upsert rows by `accountNo`
3. keep the linked `user`
4. return a normalized account list response

Balance, deposit, transfer, and transaction-history responses remain sourced from OpenAPI at request time.

## Error Handling

Rename asset exceptions to account exceptions and add account-specific error codes for:

- account product API failure
- account create API failure
- account list sync failure
- account balance API failure
- account deposit API failure
- account transfer API failure
- invalid account API response
- account not found for local user context

Global exception handling should switch from `AssetException` to `AccountException`.

## API Shape

Recommended path split:

- `/v1/admin/accounts/...`
- `/v1/accounts/...`

Recommended user flow:

1. FE calls sync endpoint
2. backend fetches OpenAPI account list and upserts local rows
3. FE reads local account list
4. FE uses account number for balance, transfer, deposit, and transaction-history calls

## Testing Strategy

Follow TDD with focused unit and slice tests:

- account controller auth/response tests
- account service/use case tests
- account sync persistence tests
- OpenAPI client validation tests

Focused tests are preferred over full integration tests because the repository currently has little existing test scaffolding.
