# CLAUDE.md — apps/api (Spring Boot backend)

Stack-specific conventions for the backend. See the repo-root `CLAUDE.md` for project-wide rules.

## Run / build

- Local (bare): `./mvnw spring-boot:run` from `apps/api` (needs Postgres — `docker compose up -d postgres`). Reads `JWT_SECRET` from `apps/api/.env` via `spring.config.import=optional:file:.env[.properties]` in `application.yml`.
- Docker: `docker compose up -d --build backend` from repo root. `JWT_SECRET` is injected via `env_file: ./apps/api/.env`.
- Format (required — CI runs `spotless:check`): `./mvnw spotless:apply`.
- Package (CI parity): `./mvnw -B -DskipTests package`.

## Secrets / config

- `apps/api/.env` (gitignored) holds `JWT_SECRET`. Never commit it. In prod, supply `JWT_SECRET` as a real env var (the `optional:` config-import is a no-op when `.env` is absent).
- Typed config lives in `AppProperties` (`app.*` block in `application.yml`): JWT TTLs, cookie flags, token TTLs.

## Schema / migrations

- Flyway owns the schema (`src/main/resources/db/migration/V*.sql`); JPA runs `ddl-auto: validate`. **Entities must match the migration exactly** — add a new `V__` migration for any schema change, never rely on Hibernate to alter tables.
- All PKs are `uuid` via Hibernate `@UuidGenerator` (app-side). Timestamps are `timestamptz` via `@CreationTimestamp`/`@UpdateTimestamp` (`Instant`).

## Auth (Phase 1 core slice — `com.orchestrate.api.auth`, `.user`, `.error`)

- **Access token**: stateless HS256 JWT (jjwt, `JwtService`), ~15 min, delivered as an httpOnly+Secure cookie (`access_token`). Validated per-request by `JwtAuthenticationFilter`.
- **Refresh token**: opaque random string, DB-backed (`refresh_tokens`), ~7 days, httpOnly+Secure cookie path-scoped to `/api/auth` (`refresh_token`). **Rotate-on-every-refresh + reuse detection**: replaying a revoked token revokes all of that user's tokens (`RefreshTokenService`).
- **Token storage rule**: never store raw tokens. Opaque tokens (refresh / email-verification / password-reset) are stored as their **SHA-256 hex** (`TokenHasher`); the raw value only lives in the cookie/link.
- **Passwords**: BCrypt (`PasswordEncoder` bean). `users.password_hash` is **nullable** — OAuth users (Phase 1b) won't have one.
- **Login is blocked until email is verified** → 403 `EMAIL_NOT_VERIFIED`. Signup issues no tokens.
- Verification/reset links are **console-logged** by `EmailLinkLogger` (placeholder) — real email provider is a fast-follow.
- Errors go through `GlobalExceptionHandler` → uniform `{error, message}` JSON. Auth failures return generic messages (no user enumeration).

## Security posture

- `SecurityConfig`: default-deny (`anyRequest().authenticated()`), stateless sessions, explicit `permitAll` only for `/api/health` + the auth entry points. New endpoints are protected by default.
- **CSRF is disabled**, currently mitigated by `SameSite=Lax` cookies. This is NOT settled for prod — see the `TODO(deploy phase)` comments in `SecurityConfig`: if frontend/backend don't share one root domain, we must move to `SameSite=None; Secure` + real CSRF protection. `app.cookies.secure` is `false` in dev and must be `true` in prod (no `application-prod.yml` yet).

## Out of scope here (later Phase 1 follow-ups)

OAuth (1b), Organizations/Workspaces/Teams/RBAC (1c), invitations (1d). Don't pull these forward without confirming. Automated auth tests need a Testcontainers Postgres setup (separate task) — current verification is curl E2E.
