# Phase 1 Backend Architecture and Invariants

This document preserves the non-obvious implementation decisions and security invariants delivered during Phase 1. It describes existing behavior, not the current work queue; `ROADMAP.md` controls phase scope. Update this document when an intentional architecture change makes an invariant obsolete.

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

## Organizations & RBAC (Phase 1c — `com.orchestrate.api.org`)

- **Entities**: `Organization` (name, unique `slug`) and `OrganizationMembership` (links a `User` to an `Organization` with a role — `OWNER`/`ADMIN`/`MEMBER`/`APPROVER`). A user can belong to multiple orgs with a different role in each; `UNIQUE(organization_id, user_id)` enforces one membership row per pair.
- **Org-scoping: URL path + per-request lookup, not JWT-embedded.** `/api/orgs/{orgId}/...`; `OrgSecurity` (referenced from `@PreAuthorize` SpEL as `@orgSecurity`) does a DB lookup of the caller's membership/role on every request. Role changes take effect immediately — no "active org" concept or token reissuance.
- **RBAC via `@EnableMethodSecurity` + `@PreAuthorize`** (added to `SecurityConfig`), not a hand-rolled filter. Every mutating endpoint (`POST/PATCH/DELETE` under `/{orgId}/members`) is gated `@orgSecurity.hasAtLeastRole(#orgId, principal, ADMIN)`; read endpoints are gated `@orgSecurity.isMember(...)`. `OrganizationRole.isAtLeast` only knows relative rank (OWNER > ADMIN > {MEMBER, APPROVER} as peers) — the actual matrix logic lives in `OrgService`, not SpEL.
- **Role matrix — owner-only above admin.** ADMIN can freely add/update/remove MEMBER/APPROVER-tier members. Anything that promotes to ADMIN/OWNER, or targets someone currently ADMIN/OWNER (role-change or removal), requires the acting user to be OWNER — applied literally, **including to self-targeting** (an ADMIN can't remove or demote themselves either; there's no "leave org" self-service path in this phase).
- **Last-owner invariant, pessimistically locked.** An org must always keep ≥1 OWNER. `OrgService.requireNotLastOwner` acquires `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the org's OWNER rows (`OrganizationMembershipRepository.findByOrganizationIdAndRole`) before deciding — **not** a plain unlocked `COUNT`. A security review caught that an unlocked count lets two concurrent requests each targeting a different one of exactly two owners both observe count=2 and both proceed, leaving zero owners with no recovery path (same TOCTOU shape as the Phase 1a refresh-token reuse bug); the lock forces the second transaction to block until the first commits, so its check reflects the post-commit state.
- **Slug**: client-supplied (not server-generated), validated (`^[a-z0-9]+(-[a-z0-9]+)*$`, 3–50 chars), DB-unique with a clean 409 on collision.
- **Roster visibility is intentionally permissive**: `GET /{orgId}/members` is gated on `isMember` (any role, including MEMBER/APPROVER), not `hasAtLeastRole(ADMIN)` — every member can see the full roster including other members' emails. Deliberate design choice (matches Slack/GitHub/Notion convention), noted explicitly in `OrgController` so it doesn't read as an oversight.
- **7 endpoints total**: create org, list orgs I belong to, get org, list members, add member (`POST /{orgId}/members` — userId + role; needed since Invitations are Phase 1d and there was otherwise no way to get a second person into an org), update a member's role, remove a member.

## Invitations (Phase 1d — `com.orchestrate.api.invitation`)

- **No new-account-via-invite path.** Accepting requires the caller to already be authenticated (existing signup/verify/OAuth flow). `POST /api/invitations/accept` checks that the authenticated principal's email (normalized) matches the invitation's stored `invited_email` — mismatch is 403 `INVITATION_EMAIL_MISMATCH`, distinct from 401 `INVALID_TOKEN` (the token itself was fine; the caller just isn't the invited identity).
- **Reuses the Phase 1a opaque-token pattern exactly**: `TokenHasher` (SHA-256 hex, raw token never stored) and `EmailLinkLogger.sendOrgInvitationLink` (console-logged, same placeholder-for-real-email-provider approach). TTL is `app.tokens.org-invitation-ttl` = `P7D` — deliberately longer than email-verification (24h) / password-reset (1h), since invite turnaround depends on a second person's response time, not a single security-sensitive action by the same user.
- **Reissue on re-invite.** A new invitation to an (org, email) pair that already has an active pending invitation revokes the old one (`revoked_at`) and issues a fresh token + TTL, rather than rejecting. At most one active pending invitation per (org, email), backed by a partial unique index (`org_invitations`, `WHERE used_at IS NULL AND revoked_at IS NULL`) as a DB-level backstop for the app-level check — same defense-in-depth shape as `organization_memberships`' `UNIQUE(organization_id, user_id)`.
- **`InvitationRepository` extends `JpaRepository`, not `CrudRepository`** (unlike the auth token repositories) because the reissue path needs `saveAndFlush`: Hibernate's default flush order runs INSERTs before UPDATEs regardless of call order, so revoking the old invitation must be flushed before the new one is inserted, or the INSERT collides with the partial unique index.
- **Three-state tracking** (`used_at` / `revoked_at`, both nullable, tracked separately) rather than the single `used_at` the auth tokens use — invitations need to distinguish accepted vs. cancelled vs. expired for the pending-invitations list and audit trail. `isUsable()` (accept-time, expiry-aware) and `isPending()` (revoke/list-eligibility, deliberately expiry-agnostic — an admin can see and revoke an expired invite) are both on `Invitation`.
- **Owner-tier matrix reused for both create and revoke**, mirroring `OrgService.addMember`'s shape: inviting (or revoking an invitation) at ADMIN/OWNER role requires the acting user to be OWNER. Since an invitation has no existing membership/current-role, both checks key off the invitation's stored (invited) role rather than a target's current role.
- **Endpoints**: `POST/GET /api/orgs/{orgId}/invitations` (create/list, gated `hasAtLeastRole(ADMIN)` — unlike the roster, who's been invited is admin-sensitive, not general-member-visible), `DELETE /api/orgs/{orgId}/invitations/{invitationId}` (revoke, same gate), `POST /api/invitations/accept` (flat, not org-scoped — mirrors `/api/auth/verify-email` — plain-authenticated only).
- Inviting an email that already belongs to an org member is rejected at creation time by reusing `MembershipAlreadyExistsException` (409) — no new exception type needed.

## Security posture

- `SecurityConfig`: default-deny (`anyRequest().authenticated()`), stateless sessions, explicit `permitAll` only for `/api/health` + the auth entry points. New endpoints are protected by default.
- **CSRF is disabled**, currently mitigated by `SameSite=Lax` cookies. This is NOT settled for prod — see the `TODO(deploy phase)` comments in `SecurityConfig`: if frontend/backend don't share one root domain, we must move to `SameSite=None; Secure` + real CSRF protection. `app.cookies.secure` is `false` in dev and must be `true` in prod (no `application-prod.yml` yet).

## Out of scope here (later Phase 1 follow-ups)

Workspaces, Teams, org settings beyond name/slug, org deletion. Don't pull these forward without confirming. Automated auth tests need a Testcontainers Postgres setup (separate task) — current verification is curl E2E.
