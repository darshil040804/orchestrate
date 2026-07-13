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
- **Passwords**: BCrypt (`PasswordEncoder` bean). `users.password_hash` is **nullable** — used by OAuth users (Phase 1b, see below).
- **Login is blocked until email is verified** → 403 `EMAIL_NOT_VERIFIED`. Signup issues no tokens.
- Verification/reset links are **console-logged** by `EmailLinkLogger` (placeholder) — real email provider is a fast-follow.
- Errors go through `GlobalExceptionHandler` → uniform `{error, message}` JSON. Auth failures return generic messages (no user enumeration).

## OAuth (Phase 1b — Google + GitHub, `com.orchestrate.api.auth.oauth`)

- Layered on top of the same token machinery as password login — no parallel auth mechanism. `AuthService.loginOrSignupViaOAuth(email)` finds-or-creates the `User` (nullable `password_hash`, `emailVerified=true` immediately) and calls the exact same private `issueTokens(User)` that password `login()` uses, then `OAuthAuthenticationSuccessHandler` sets cookies via the same `CookieFactory` and redirects to `app.frontend-url`.
- **Account linking: auto-link by verified email.** An OAuth login for an email that already has a `User` row (password-based or another OAuth provider) reuses that account — no rejection, no explicit linking step.
- **GitHub's `/user` endpoint often returns a null email** even with the `user:email` scope granted (only public profile emails show there). `GitHubOAuth2UserService` falls back to `GET /user/emails`, requires a **verified** email (preferring `primary`), and fails the login outright if none exists — the "never authenticate without a verified email" rule from Phase 1a applies to OAuth too. Google's OIDC `email_verified` claim is defensively re-checked in `OAuthAuthenticationSuccessHandler` for the same reason.
- Both "no verified email" detection points (GitHub's pre-auth exception, Google's post-auth check) reference the same `OAuthErrorCodes.UNVERIFIED_EMAIL` constant rather than each hardcoding the redirect string — see that class's javadoc for why.
- **Session model**: a second `SecurityFilterChain` (`@Order(1)`, matched only to `/oauth2/**` and `/login/oauth2/**`) uses Spring's *default* session-backed authorization-request repository to survive the redirect round-trip to Google/GitHub and back — the main API chain (`@Order(2)`) stays fully `STATELESS` as before. See the `TODO(scale-out)` comment on `oauth2LoginFilterChain`: this relies on in-memory `HttpSession`, fine for a single instance, needs a shared session store if ever scaled horizontally.
- No new REST endpoints: `.oauth2Login(...)` auto-registers `/oauth2/authorization/{google,github}` (initiate) and `/login/oauth2/code/{google,github}` (callback). Those two exact URLs are what must be registered as the redirect/callback URI in the Google Cloud Console / GitHub OAuth App settings.
- Client id/secret come from `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`/`GITHUB_CLIENT_ID`/`GITHUB_CLIENT_SECRET` in `apps/api/.env` (same mechanism as `JWT_SECRET`).

## Security posture

- `SecurityConfig`: default-deny (`anyRequest().authenticated()`), stateless sessions, explicit `permitAll` only for `/api/health` + the auth entry points. New endpoints are protected by default.
- **CSRF is disabled**, currently mitigated by `SameSite=Lax` cookies. This is NOT settled for prod — see the `TODO(deploy phase)` comments in `SecurityConfig`: if frontend/backend don't share one root domain, we must move to `SameSite=None; Secure` + real CSRF protection. `app.cookies.secure` is `false` in dev and must be `true` in prod (no `application-prod.yml` yet).

## Out of scope here (later Phase 1 follow-ups)

Organizations/Workspaces/Teams/RBAC (1c), invitations (1d). Don't pull these forward without confirming. Automated auth tests need a Testcontainers Postgres setup (separate task) — current verification is curl E2E.
