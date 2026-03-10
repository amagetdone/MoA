# Backend Collaboration Guide

## Purpose

This document is the shared reference for backend developers and external coding agents working in this repository.

The goal is simple:

- keep package placement consistent
- keep responsibilities separated by layer
- keep new code aligned with standard Java and Spring conventions

When this guide and an individual preference conflict, this guide wins.

## Top-Level Package Structure

All backend code must stay under:

```text
com.ssafy.amagetdone
├── config
├── domain
└── global
```

### `config`

Application-wide configuration belongs here.

- Spring configuration classes
- environment loading
- infrastructure setup shared by the entire application

Do not place business logic in `config`.

### `global`

Cross-domain shared code belongs here.

- common base entities
- global exception handling
- shared response format
- common annotations, filters, interceptors, utilities

Do not move domain-specific policy or feature logic into `global`.

Exception handling rules:

- Global exception infrastructure belongs under `global/exception`.
- Shared contracts such as `GlobalExceptionHandler`, `ErrorResponse`, and `ErrorCode` must be defined there.
- `global` may translate exceptions into HTTP responses, but it must not own domain-specific error codes.

### `domain`

All business code must live under a dedicated domain package.

Examples already present in this repository:

- `asset`
- `budget`
- `member`
- `plan`
- `transaction`

Do not create feature code directly under `com.ssafy.amagetdone`.

## Standard Domain Package Structure

Each domain follows this shape:

```text
domain/<domain-name>
├── api
│   ├── controller
│   └── dto
├── application
│   ├── service
│   └── usecase
├── infra
│   ├── client
│   ├── mapper
│   └── persistence
├── model
│   ├── entity
│   ├── repository
│   └── vo
└── support
    └── util
```

This structure is the default. New files should be added to the existing layer instead of creating a new arbitrary subpackage.

## Layer Responsibilities

### `api`

External request and response handling belongs here.

- `controller`: REST endpoints
- `dto`: request and response DTOs

Rules:

- Controllers must stay thin.
- Controllers should validate input, call the application layer, and return a response.
- Controllers must not directly call JPA repositories.
- Controllers must not contain business decision logic.
- In `api/dto`, each API should own exactly one DTO file.
- That DTO file should contain the request and response types together as nested types such as `Request` and `Response`.
- Do not split one API's request and response into separate DTO files unless there is an explicit repository-wide exception.

### `application`

Application flow and business execution belong here.

#### `application/usecase`

Implement feature-level business modules here.

- one use case should represent one business action or one clear unit of behavior
- use cases should be reusable
- complex feature logic should be extracted here first

Examples:

- `CreateBudgetUseCase`
- `AdjustBudgetUseCase`
- `SyncTransactionsUseCase`

#### `application/service`

Services orchestrate use cases.

- combine one or more use cases into a complete scenario
- define transaction boundaries when needed
- coordinate flow between domain logic and infrastructure dependencies

Services are not the place to accumulate every line of business logic. The preferred pattern is:

1. implement the actual feature behavior in `usecase`
2. inject that use case into `service`
3. let the service compose the scenario used by the controller

This keeps feature logic modular and easier to test or reuse.

### `infra`

Technical integration code belongs here.

- `client`: external API or external system clients
- `mapper`: mapping between external payloads and internal models
- `persistence`: persistence-specific implementation details

Rules:

- keep infrastructure concerns out of controllers
- avoid mixing external API parsing logic into entities or services

### `model`

Core domain model types belong here.

- `entity`: JPA entities
- `repository`: domain repositories, including Spring Data JPA interfaces
- `vo`: enums and value objects

Rules:

- entities represent persisted domain state
- repositories belong next to the model, not in controller or service packages
- enums and value objects should stay close to the domain that owns them

### `support`

Small domain-local helpers belong here.

Use `support` only for logic that is local to one domain and does not belong in `global`.

## Exception Handling Structure

Use the following structure for exception handling:

```text
global/exception/
  GlobalExceptionHandler.java
  ErrorResponse.java
  ErrorCode.java

domain/<domain-name>/exception/
  <Domain>ErrorCode.java
  <Domain>Exception.java
```

Rules:

- Keep exactly one global exception handler in `global/exception`.
- Keep the common error response format and error code contract in `global/exception`.
- Define domain-specific error codes in each domain's own exception package.
- Define domain-specific runtime exceptions in each domain's own exception package.
- Do not create one giant shared error-code enum across all domains.
- Do not place one domain's error codes in another domain or in `global`.
- When multiple developers work in parallel, they should add or edit only their domain's `*ErrorCode` and `*Exception` files whenever possible.
- Prefer throwing domain exceptions such as `MemberException` or `AssetException` from domain code instead of throwing `ResponseStatusException` directly.

## Recommended Development Flow

When implementing a new backend feature, use this order:

1. define the controller request and response DTOs in `api/dto`
2. create or update the controller in `api/controller`
3. implement the feature unit in `application/usecase`
4. compose the feature flow in `application/service`
5. use repositories, clients, and mappers from `model` or `infra`

Preferred dependency flow:

```text
Controller -> Service -> UseCase -> Repository/Client
```

Not preferred:

```text
Controller -> Repository
Controller -> Entity
Service with all business logic inline and no use case separation
```

## UseCase and Service Pattern

Use the following rule when deciding where code should go.

### Put code in `usecase` when:

- it represents one business capability
- it may be reused by another scenario
- it contains actual business rules or decision logic

### Put code in `service` when:

- it coordinates several use cases
- it defines a single application scenario exposed to controllers
- it manages transaction boundaries or execution order

### Example

```java
package com.ssafy.amagetdone.domain.budget.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.amagetdone.domain.budget.application.usecase.AdjustBudgetUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.CreateBudgetUseCase;

@Service
public class BudgetService {

	private final CreateBudgetUseCase createBudgetUseCase;
	private final AdjustBudgetUseCase adjustBudgetUseCase;

	public BudgetService(
		CreateBudgetUseCase createBudgetUseCase,
		AdjustBudgetUseCase adjustBudgetUseCase
	) {
		this.createBudgetUseCase = createBudgetUseCase;
		this.adjustBudgetUseCase = adjustBudgetUseCase;
	}

	@Transactional
	public void openMonthlyBudget(OpenBudgetCommand command) {
		createBudgetUseCase.execute(command);
		adjustBudgetUseCase.applyOpeningAdjustment(command);
	}
}
```

The point of this pattern is not to increase class count for no reason. The point is to stop large services from becoming a single hard-to-maintain business blob.

## Code Conventions

Use standard Java and Spring conventions by default. When repository-specific style already exists, follow the repository.

### Naming

- package names: lowercase only
- domain package names: singular lowercase nouns such as `budget`, `member`, `plan`
- class and enum names: `PascalCase`
- methods and fields: `camelCase`
- constants: `UPPER_SNAKE_CASE`
- repository interfaces: `<EntityName>Repository`
- use case classes: `<Action><Domain>UseCase`
- service classes: `<Domain>Service`
- controller classes: `<Domain>Controller`
- DTO file names: explicit API names such as `SocialAuthDto`, `ProfileCompletionDto`
- Nested DTO type names: `Request`, `Response`
- domain error code enums: `<Domain>ErrorCode`
- domain exception classes: `<Domain>Exception`

### Java and Spring style

- keep one public class per file
- prefer constructor injection
- keep controllers thin and stateless
- keep entities persistence-focused
- place transaction boundaries in the application layer, usually in services
- avoid field injection
- avoid wildcard imports

### JPA style

- entities belong in `model/entity`
- repositories belong in `model/repository`
- enums and value objects belong in `model/vo`
- shared entity fields belong in `global/entity`
- table and column mappings should be explicit when the schema is fixed

### Formatting

- follow the existing repository formatting when editing existing files
- preserve current indentation and import ordering in touched packages
- do not mix unrelated reformatting with feature changes

## Placement Rules

### Good

- account entity -> `domain/asset/model/entity/Account.java`
- budget adjustment enum -> `domain/budget/model/vo/BudgetAdjustmentType.java`
- health check controller -> `global/controller/GlobalHealthCheckController.java`
- transaction sync orchestration -> `domain/transaction/application/service`
- reusable transaction import logic -> `domain/transaction/application/usecase`

### Bad

- putting repository calls directly in a controller
- putting domain-specific helper logic in `global`
- placing DTOs in `model`
- placing external API response parsing in `service`
- creating a new top-level package because a file does not seem to fit immediately

## Rules for Agents

If you are an agent generating or editing code in this repository, follow these rules before creating files.

1. identify the owning domain first
2. place the file in the existing standard layer
3. if the behavior is a feature module, create or reuse a `usecase`
4. if the behavior is an orchestration scenario, implement it in `service`
5. do not introduce a new top-level package without team agreement
6. do not bypass the layering rules for convenience

When uncertain, prefer consistency with the current package structure over inventing a new pattern.
