# Backend Guide

Apply the root `AGENTS.md` first. For the rationale behind Phase 1 security and domain decisions, read `../../docs/architecture/phase-1-backend-invariants.md` only when the change touches those areas.

## Commands

From `apps/api`:

- `./mvnw spring-boot:run`: run locally; start PostgreSQL with `docker compose up -d postgres` from the repository root.
- `./mvnw spotless:apply`: apply Google Java Format.
- `./mvnw spotless:check test`: required verification.
- `./mvnw -B -DskipTests package`: package using the CI build shape.

Use `mvnw.cmd` on Windows.

## Architecture

- Organize code by feature under `com.orchestrate.api`.
- Keep controllers limited to HTTP concerns, validation, and response mapping.
- Put business rules and transaction boundaries in services.
- Use DTOs at API boundaries; do not expose JPA entities as contracts.
- Keep persistence access in repositories and avoid N+1 query patterns.
- Use `GlobalExceptionHandler` and the established error shape; do not create endpoint-specific error formats.
- Preserve backward compatibility unless the requested change explicitly permits a breaking contract.

## Persistence

- Flyway owns the schema; JPA uses `ddl-auto: validate`.
- Add a new `src/main/resources/db/migration/V*__description.sql` for every schema change. Never edit an applied migration.
- Entities must exactly match migrations.
- Primary keys use UUIDs and persisted timestamps use `Instant`/`timestamptz`.
- Treat concurrency-sensitive invariants transactionally and test simultaneous operations. Do not replace locking with an unlocked check-then-write sequence.

## Security Invariants

- Preserve default-deny security: new endpoints require authentication unless explicitly documented as public.
- Keep the main API stateless. OAuth redirect endpoints are the deliberate session-backed exception.
- Never store or log raw refresh, verification, reset, or invitation tokens. Store only `TokenHasher` SHA-256 hashes.
- Preserve refresh-token rotation and reuse detection.
- Require verified email for password and OAuth authentication; keep authentication errors generic to prevent user enumeration.
- Reuse the existing token, cookie, and authentication services rather than creating parallel mechanisms.
- Keep passwords encoded with BCrypt.
- Preserve secure/HttpOnly cookie behavior. Do not weaken cookie, CORS, CSRF, or trusted-proxy settings for convenience.
- Never commit `apps/api/.env`. Secrets must come from environment configuration.

## Organization and Invitation Invariants

- Organization authorization is derived from current membership, not JWT-embedded roles.
- Apply RBAC through method security and `OrgSecurity`; do not duplicate role checks in controllers.
- OWNER-only rules above the ADMIN tier apply to self-targeting operations as well.
- Every organization must retain at least one OWNER. Preserve the pessimistic-locking protection around the last-owner check.
- Invitation acceptance requires an authenticated account whose normalized email matches the invited email.
- Re-inviting revokes and flushes the existing active invitation before inserting the replacement.
- Preserve the database uniqueness constraints that backstop membership and invitation invariants.

## Configuration

- Typed application configuration belongs in `AppProperties` and the `app.*` configuration block.
- Document every new environment variable and provide a safe local example.
- Production secrets must be injected as environment variables.
- Do not modify or create production configuration speculatively.

## Testing

- Name tests `*Tests.java` and place them under the matching package in `src/test/java`.
- Add focused unit tests for business rules and integration tests where security, transactions, persistence, migrations, or concurrency matter.
- Cover success, authorization failure, invalid input, conflicts, and relevant concurrent behavior.
- Run `./mvnw spotless:check test` before completion and report any check that could not run.
