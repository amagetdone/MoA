# Domain Entity Design

**Context**

Column-level schema must follow [`docs/plans/2026-03-10-domain-entity-erd-spec.md`](docs/plans/2026-03-10-domain-entity-erd-spec.md).
The ERD image remains the ultimate source, and the spec document is the written transcription checkpoint used during implementation.

The package structure has already been fixed:

- `global`
- `config`
- `domain`

Each domain uses:

- `api/controller`
- `api/dto`
- `application/service`
- `application/usecase`
- `model/entity`
- `model/repository`
- `model/vo`
- `infra/client`
- `infra/persistence`
- `infra/mapper`
- `support/util`

The ERD at `/mnt/c/ssafy/특화/docs/erd/erd.png` is the source of truth. Entity design must follow the ERD directly rather than simplifying based on implementation preference.
Do not omit columns, defaults, or relationships listed in the ERD spec document.

**Decision**

Adopt these domains:

- `member`
- `asset`
- `transaction`
- `plan`
- `budget`

This split reflects the actual product boundary:

- `member`: user identity, login method, settings, policy, agreements
- `asset`: owned resource metadata such as account, card, pocket, category, merchant
- `transaction`: internal mirrored transaction and override logic used for analysis and tracking
- `plan`: savings goals and weekly goal allocation
- `budget`: monthly and weekly budgeting, essential allocation, rollover, and adjustment history

**Entity-to-Domain Mapping**

`member`

- `User`
- `SocialAccount`
- `Setting`
- `Agreement`
- `Policy`

`asset`

- `Account`
- `Card`
- `Pocket`
- `Category`
- `Merchant`

`transaction`

- `TransactionOverride`
- `CardTransactionOverride`
- `AccountTransactionOverride`
- future internal transaction ledger entities

`plan`

- `Plan`
- `PlanWeekAllocation`

`budget`

- `BudgetMonth`
- `EssentialExpenseMonth`
- `BudgetWeek`
- `EssentialWeekAllocation`
- `BudgetAdjustment`
- `BudgetRollover`

**Key Rules**

- Follow the ERD field set and relationships as-is unless the ERD is revised.
- Follow the written ERD transcription in `2026-03-10-domain-entity-erd-spec.md` table-by-table while implementing.
- Do not collapse `SocialAccount` into `User`.
- Keep `Setting` under `member`; it is expected to grow beyond simple MVP configuration.
- Keep `transaction` separate from `asset`; the project mirrors external account-system data internally for analysis and tracking.
- Keep enums in `model/vo` near the owning domain.
- `BudgetAdjustmentType` must include:
  - `FREE_BUDGET_ADD`
  - `FREE_BUDGET_SUBTRACT`
  - `PLAN_ADD`
  - `PLAN_SUBTRACT`
  - `PLAN_DELAY`
  - `PLAN_EARLY_CLOSE`
  - `ROLLOVER_APPLIED`

**Implementation Strategy**

The full ERD remains the MVP reference, but entity implementation should proceed in dependency order:

1. `member`
2. `asset`
3. `transaction`
4. `plan`
5. `budget`

The implementation order is only about execution safety. It does not change the canonical ERD.
