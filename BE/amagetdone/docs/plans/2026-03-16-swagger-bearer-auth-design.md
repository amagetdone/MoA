# Swagger Bearer Auth Design

**Context**

The backend already exposes Swagger UI through springdoc and keeps the UI publicly reachable only in the `dev` and `test` profiles. Runtime authentication for protected APIs is handled by `SecurityConfig` and `JwtAuthenticationFilter`, while `/v1/auth/social/test` already returns a valid mocked `accessToken` and `refreshToken`.

The missing piece is OpenAPI documentation support for Bearer authentication, so a developer can call `/v1/auth/social/test`, copy the returned `accessToken`, click `Authorize` in Swagger UI, and then test protected endpoints without changing the existing authentication flow.

**Confirmed Constraints**

- Swagger UI access rules remain unchanged. It stays publicly reachable only in `dev` and `test`.
- OpenAPI documentation should always describe Bearer authentication, regardless of profile.
- `/v1/auth/social/test` remains the source of mocked credentials. No new mock auth endpoint is introduced.
- Runtime security behavior must not change for non-Swagger clients.
- Public auth endpoints must stay documented as public.

**Approach Options**

1. Add a global Bearer security scheme and mark only protected API docs with security requirements.
   - Pros: standard OpenAPI model, accurate per-endpoint docs, small implementation scope.
   - Cons: token still needs to be copied manually from `/v1/auth/social/test`.

2. Add a global Bearer security scheme for every endpoint.
   - Pros: smallest code change.
   - Cons: public auth endpoints look protected in docs, which is misleading.

3. Add Swagger UI customization that calls `/v1/auth/social/test` and auto-fills `Authorize`.
   - Pros: best UX.
   - Cons: requires custom UI behavior and is larger than the current need.

**Chosen Approach**

Use option 1.

**Design**

1. Extend `SwaggerConfig` with an HTTP Bearer security scheme.
   - Register a `bearerAuth` scheme under `components.securitySchemes`.
   - Use `type=http`, `scheme=bearer`, and `bearerFormat=JWT`.

2. Declare security requirements only on protected API documentation surfaces.
   - Add `@SecurityRequirement(name = "bearerAuth")` to docs interfaces or controllers that represent authenticated endpoints.
   - Keep `AuthDocsController` public because `/v1/auth/social/test` and related auth endpoints must remain callable without prior authorization.

3. Preserve the current runtime authentication path.
   - `SecurityConfig` and `JwtAuthenticationFilter` are not changed for this feature unless tests prove a documentation-related integration issue.
   - Swagger authorization is documentation-only metadata that sends the same `Authorization: Bearer <token>` header the backend already accepts.

4. Developer usage flow in Swagger UI.
   - Call `POST /v1/auth/social/test`.
   - Copy `data.accessToken` from the response.
   - Click `Authorize` in Swagger UI and paste the token.
   - Execute protected endpoints such as `/transactions` or `/v1/members/me/profile`.

**Files Expected To Change**

- `src/main/java/com/ssafy/amagetdone/global/config/SwaggerConfig.java`
- `src/main/java/com/ssafy/amagetdone/global/docs/controller/BudgetDocsController.java`
- `src/main/java/com/ssafy/amagetdone/global/docs/controller/MemberProfileDocsController.java`
- `src/main/java/com/ssafy/amagetdone/global/docs/controller/TransactionDocsController.java`
- `src/main/java/com/ssafy/amagetdone/domain/plan/api/controller/PlanImageController.java`
- `src/test/java/com/ssafy/amagetdone/global/config/SwaggerConfigIntegrationTest.java`

`AuthDocsController` is intentionally excluded from security requirements.

**Testing Strategy**

- Add or extend an integration test on `/api/v3/api-docs`.
- Verify the Bearer security scheme exists under `components.securitySchemes`.
- Verify protected endpoints expose a security requirement in the generated OpenAPI document.
- Verify `/v1/auth/social/test` remains documented without a security requirement.

**Out of Scope**

- Auto-filling Swagger `Authorize` from the `/v1/auth/social/test` response
- Any new mock authentication filter
- Any change to JWT parsing, token issuing, or public endpoint rules
