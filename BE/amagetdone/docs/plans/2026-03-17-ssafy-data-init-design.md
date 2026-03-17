# SSAFY Data Init Design

## Goal
Add an authenticated admin endpoint at `/v1/admin/accounts/data-init` that accepts an SSAFY admin `apiKey` and target user `userApiKey`, then initializes one SSAFY demand-deposit account, one linked credit card, a small preset of account transactions, and a small preset of card transactions while immediately persisting the resulting local account, card, and transaction override records.

## Scope
- Reuse the existing `domain/account` admin surface instead of introducing a new domain.
- Keep the controller thin and push orchestration into a dedicated use case.
- Use preset product IDs, card product IDs, merchant IDs, and transaction amounts defined in code.
- Persist local records immediately from creation responses instead of relying on a later sync job.

## Architecture
- Extend `AccountAdminController` with `POST /data-init`.
- Add request/response DTOs to `AccountAdminDto`.
- Add `AccountService.initializeDummyData(...)` as a thin delegation layer.
- Implement `InitializeSsafyDummyDataUseCase` to orchestrate:
  1. resolve the local user by `userApiKey`
  2. create the primary demand-deposit account in SSAFY
  3. persist the local `Account`
  4. create preset demand-deposit transactions
  5. create a credit card using the newly created account
  6. persist the local `Card`
  7. create preset card transactions
  8. persist `AccountTransactionOverride` and `CardTransactionOverride`

## External API Strategy
- Keep runtime `apiKey` usage isolated to new admin-only SSAFY clients under `domain/account/infra/client`.
- Use the existing `SsafyApiRequestFactory`, which already supports caller-provided `apiKey` and `userKey`.
- Add admin DTOs for:
  - demand-deposit account creation
  - demand-deposit deposit/transfer transaction creation
  - credit-card creation
  - credit-card transaction creation

## Data Strategy
- Use a single persisted primary account.
- Create one temporary remote-only helper account to generate withdrawal history for the primary account.
- Do not persist the helper account locally.
- Persist local transactions directly from the create-transaction API responses.
- Reuse existing repositories:
  - `AccountRepository`
  - `CardRepository`
  - `CategoryRepository`
  - `MerchantRepository`
  - `AccountTransactionOverrideRepository`
  - `CardTransactionOverrideRepository`

## Preset Scenario
- Demand-deposit product: fixed to the agreed preset ID in code.
- Credit-card product: fixed to the agreed preset ID in code.
- One account transaction seed set:
  - 2 deposits
  - 2 withdrawals via transfer to the helper account
- One card transaction seed set:
  - 4 merchant purchases using the provided preset merchant IDs

## Error Handling
- Wrap local persistence in a single `@Transactional` use case.
- Fail fast on:
  - missing local user for `userApiKey`
  - blank keys
  - unsupported preset IDs
  - SSAFY API failures
  - malformed SSAFY responses
- Accept that remote resources may remain created if a later step fails; no compensation flow is added because the SSAFY docs in scope do not provide a matching delete path.
- Avoid logging raw `apiKey`.

## Testing
- Controller unit test for response wrapping and delegation.
- Use case unit tests for:
  - happy path sequencing
  - primary account persisted, helper account not persisted
  - card created after account creation
  - account/card transactions saved immediately
  - local rollback path on intermediate failure

## Known Risk
- The current `Card.cardUniqueNo` field is typed as `Long`, while SSAFY card product/card identifiers appear string-based in the docs. Implementation must confirm whether an existing numeric contract is already assumed elsewhere or whether the entity and related repository/query types need to be aligned.
