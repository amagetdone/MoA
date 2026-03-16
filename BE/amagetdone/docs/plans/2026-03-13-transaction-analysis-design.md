# Transaction Analysis Design

**Context**

The project already has SSAFY-shaped transaction entities under `domain.transaction.model.entity` and budget entities under `domain.budget.model.entity`. The requested work adds transaction synchronization, consumption analysis, and weekly essential-expense recommendations without changing the existing transaction table shape or touching the current budget tables.

**Confirmed Constraints**

- API paths do not use `/v1`. The application still runs behind the existing `/api` servlet context path.
- `TransactionOverride`, `AccountTransactionOverride`, and `CardTransactionOverride` remain the raw transaction store.
- Existing budget tables are read-only for this feature. No column changes and no behavior changes are allowed there.
- New persistence is allowed only for analysis results, job tracking, and essential-expense snapshots.
- Weekly essential expense is derived from the latest 3 months of analyzed consumption.
- Monthly essential expense is a presentation-level sum of weekly essential expense snapshots.
- Transaction list reads must trigger synchronization before returning data.
- Analysis jobs must also perform synchronization before analysis starts.
- In weekly budget setup, this work owns only the essential-expense section. Saving and free-spendable sections belong to another teammate.

**API Surface**

The current implementation scope covers these endpoints.

1. `GET /transactions/expenses/summary`
   - Returns graph-ready expense totals for a period.
   - Default range is the latest 6 months.
   - Supports `fromMonth`, `toMonth`, and `bucket=MONTH|WEEK`.
   - Includes only analyzed consumption. Income and internal transfers are excluded.

2. `GET /transactions/expenses/monthly`
   - Returns total consumption for a specific month.
   - Supports `month=YYYY-MM`.
   - Includes total expense, fixed expense, variable expense, excluded income amount, excluded internal transfer amount, and transaction count.

3. `GET /transactions`
   - Triggers latest synchronization for the authenticated user before reading data.
   - Returns normalized transaction list data from the existing transaction tables plus analysis metadata.
   - Supports `fromDate`, `toDate`, `sourceType`, `analysisInclusion`, `page`, and `size`.

4. `POST /transactions/analysis-jobs`
   - Starts an asynchronous `sync + analysis` job.
   - Defaults to `months=6`.
   - Rejects duplicate running jobs for the same user.

5. `GET /transactions/analysis-jobs/{jobId}`
   - Returns asynchronous job status and step-level progress.

6. `GET /transactions/analysis/weeks/{yearWeek}`
   - Returns weekly consumption analysis for the requested ISO week.
   - Includes weekly expense amount, weekly essential amount, fixed and variable essential breakdown, and category-level spending.

7. `GET /transactions/analysis/months/{yearMonth}`
   - Returns monthly consumption analysis for the requested month.
   - Monthly essential amount is derived by summing weekly essential snapshots inside the month.

8. `GET /budgets/weeks/{yearWeek}/essential-expenses`
   - Returns the essential-expense section used by the weekly budget setup screen.
   - Includes `essentialAmount`, `fixedEssentialAmount`, `variableEssentialAmount`, `fixedExpenseCategories`, and `variableExpenseCategories`.

The final screen-level facade is intentionally split from the current ownership boundary.

- `GET /budgets/weeks/{yearWeek}/setup`
  - Aggregates `essentialBudget`, `savingBudget`, and `freeSpendable`.
  - Only the `essentialBudget` section is part of this work.
  - `savingBudget` and `freeSpendable` remain integration points for another teammate.

**Synchronization Design**

Synchronization always starts from the authenticated user.

1. Read `userId` from the access token using the existing token reader approach.
2. Load `User.userKey`, all linked `Account`s, and all linked `Card`s.
3. For each account, load the latest stored `AccountTransactionOverride`.
4. For each card, load the latest stored `CardTransactionOverride`.
5. Call SSAFY transaction APIs in descending order and inspect the newest remote transaction.
6. If the newest remote transaction matches the newest local transaction for that source, skip that source.
7. If the newest remote transaction differs, reload transactions from the latest local transaction date through today and insert only missing rows into the existing transaction tables.
8. If no local transaction exists for the source, seed the requested range directly.

The synchronization fingerprint is source-specific and compares:

- source type
- source identifier
- `transactionUniqueNo`
- occurred date and time
- transaction amount

The raw storage remains the current table structure:

- `TransactionOverride`
- `AccountTransactionOverride`
- `CardTransactionOverride`

No new raw transaction table or sync cursor table is introduced.

**Analysis Pipeline**

The asynchronous analysis pipeline runs after synchronization succeeds.

1. Load the latest N months of existing transaction rows for the user. Default N is 6.
2. Normalize merchant text.
   - Card rows use `merchantName`.
   - Account rows use `transactionSummary`, then `transactionMemo` as fallback.
3. Batch-call `POST https://j14d101.p.ssafy.io/inference/merchant/batch`.
4. Map `final_label` into the internal category taxonomy.
5. Apply exclusion rules.
   - Deposit-side demand deposit income is excluded from consumption analysis.
   - Transfers between the user's registered service accounts are excluded from both income and expense.
6. Classify included essential transactions into `FIXED` or `VARIABLE`.
7. Build weekly and monthly projections from the classified result.
8. Persist transaction-level analysis metadata and weekly essential snapshots.

**Exclusion Rules**

- Demand deposit income does not participate in consumption analysis.
- Internal transfers between service-registered accounts do not participate in income or expense totals.
- Card payments remain eligible consumption unless later excluded by policy.
- Non-essential categories can remain part of general expense analysis, but they do not contribute to weekly essential-expense recommendations.

**Fixed and Variable Essential Classification**

The classifier uses a rolling 3-month base window.

- Any transaction labeled as subscription is always `FIXED`.
- Other essential transactions are grouped by `normalizedMerchantText + categoryId`.
- A group becomes `FIXED` when:
  - the transaction appears in all 3 months, and
  - amount dispersion is within an agreed threshold, and
  - day-of-month dispersion is within an agreed threshold.
- If the group fails those checks, it is `VARIABLE`.

Weekly essential expense is derived as follows.

- Fixed essential expense is allocated to the target week when the predicted payment date falls in that week.
- Variable essential expense uses recent weekly averages from the same 3-month base window.
- Monthly essential expense is not stored as a primary source of truth. It is presented as the sum of weekly essential snapshots for the month.

**Persistence Design**

New tables are limited to analysis and essential-expense snapshots.

1. `transaction_analysis_job`
   - Stores async job lifecycle and failure details.
   - One user can have only one running job at a time.

2. `transaction_analysis_result`
   - Stores per-transaction analysis output.
   - FK to `transaction_override`.
   - Stores category, inclusion flag, excluded reason, expense scope, and inference metadata.

3. `essential_expense_snapshot_week`
   - Stores the weekly essential-expense recommendation snapshot.
   - Stores week range, base month count, total essential amount, fixed essential amount, variable essential amount, and analyzed time.

4. `essential_expense_snapshot_week_category`
   - Stores category-level weekly essential amounts.
   - FK to `essential_expense_snapshot_week`.
   - Stores category, scope, and amount.

Existing budget tables remain untouched and can consume the new snapshot data later through read-side composition.

**Domain Responsibilities**

`domain.transaction`

- SSAFY synchronization
- transaction listing
- expense summary and monthly expense queries
- async analysis job orchestration
- transaction-level labeling and exclusion persistence
- weekly and monthly analysis read models

`domain.budget`

- weekly essential-expense read API for budget setup
- composition of essential-expense snapshot data into budget-facing DTOs
- no schema or behavior change to current budget tables

**Recommended Components**

Under `domain.transaction`:

- `TransactionSyncService`
- `TransactionAnalysisOrchestrator`
- `MerchantLabelingClient`
- `TransactionExclusionPolicy`
- `FixedExpenseClassifier`
- `ExpenseSummaryQueryService`

Under `domain.budget`:

- `EssentialExpenseSnapshotQueryService`
- `WeeklyEssentialExpenseFacade`

**Error Handling**

- Use `TransactionException` and domain-specific error codes for SSAFY and inference failures.
- Return `202 Accepted` for analysis-job creation.
- For transaction-list reads, return cached DB data with sync metadata when a partial sync failure occurs and local data already exists.
- For analysis jobs, fail the job when synchronization or inference fails because stale analysis is worse than delayed analysis.

**Testing Strategy**

- Repository and entity mapping tests for new analysis and snapshot entities.
- Client tests for the merchant inference batch API wrapper.
- Use-case tests for sync decision logic, exclusion policy, fixed-expense classification, and weekly allocation.
- Controller tests for:
  - `/transactions/expenses/summary`
  - `/transactions/expenses/monthly`
  - `/transactions`
  - `/transactions/analysis-jobs`
  - `/transactions/analysis-jobs/{jobId}`
  - `/transactions/analysis/weeks/{yearWeek}`
  - `/transactions/analysis/months/{yearMonth}`
  - `/budgets/weeks/{yearWeek}/essential-expenses`

**Out of Scope**

- Saving-budget recommendation logic
- Free-spendable amount logic
- Final implementation of `GET /budgets/weeks/{yearWeek}/setup`
- `GET /budgets/weeks/{yearWeek}/setup` remains a teammate-integration facade and is intentionally left out of this implementation scope
- Any schema change to existing budget tables
- Replacing the current SSAFY-aligned transaction table design
