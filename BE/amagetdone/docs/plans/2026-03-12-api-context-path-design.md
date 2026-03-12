# API Context Path Design

**Goal**

Move the `/api` prefix out of individual controller mappings and make it a servlet context path so the external API contract stays stable while controller paths become cleaner.

**Chosen Approach**

- Set `server.servlet.context-path=/api` in application configuration.
- Remove the leading `/api` segment from controller mappings.
- Keep the external health endpoint at `GET /api/health` by changing the controller mapping itself to `GET /health`.
- Keep Nginx infrastructure health at `GET /healthz`.

**Alternatives Considered**

1. Keep `/api` hardcoded in controllers.
   - Simple short term, but repeats the same prefix everywhere and makes framework routing policy live in application code.
2. Keep controller mappings as-is and use Nginx rewrite rules.
   - Preserves current app code, but pushes API contract knowledge into infrastructure and creates two routing sources of truth.
3. Use servlet context path and strip `/api` from controllers.
   - Recommended and chosen because it centralizes the prefix and keeps external URLs unchanged.

**Impact**

- Existing external API paths remain `/api/v1/...`.
- Internal controller mappings become `/v1/...` and `/health`.
- Tests that exercise full request paths continue to hit `/api/...` because the servlet context path owns that prefix.
- Any hardcoded backend-local health checks must use `/api/health`.

**Operational Notes**

- Temporary compatibility aliases like a root-level `/health` are not compatible with a global servlet context path unless extra routing is added outside the main dispatcher. This change removes that alias and treats `/api/health` as the single app-health contract.
- Missing endpoints should still resolve as `404`, not `500`.

**Verification**

- Controller tests pass with external paths like `/api/v1/...` and `/api/health`.
- Full backend test suite passes.
- Local runtime returns `200` for `http://127.0.0.1:8080/api/health`.
- Local runtime no longer exposes a successful root-level `http://127.0.0.1:8080/health`.
