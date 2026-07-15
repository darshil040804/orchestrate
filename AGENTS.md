# Repository Guidelines

## Project Structure & Module Organization

This Orchestrate monorepo has a Next.js 16 frontend in `apps/web/`: routes are in `src/app/`, shared UI in `src/components/`, hooks in `src/hooks/`, utilities in `src/lib/`, and assets in `public/`. The Java 21 Spring Boot backend is in `apps/api/`; feature packages are under `src/main/java/com/orchestrate/api/`, resources and Flyway migrations (`V*__description.sql`) under `src/main/resources/`, and tests under `src/test/java/`. Read the nearest nested guide before changing an app. `ROADMAP.md` defines phase scope; do not rewrite it without agreement.

## Build, Test, and Development Commands

- `docker compose up --build`: start PostgreSQL, Redis, API, and web app.
- `docker compose up -d postgres`: start only the database for bare API development.
- `cd apps/web && npm ci && npm run dev`: install locked dependencies and run the frontend at port 3000.
- `cd apps/web && npm run lint && npm run build`: run the same web checks as CI.
- `cd apps/api && ./mvnw spring-boot:run`: run the API at port 8080 (use `mvnw.cmd` on Windows).
- `cd apps/api && ./mvnw spotless:check test`: verify Java formatting and run tests.
- `cd apps/api && ./mvnw spotless:apply`: apply Google Java Format.

## Coding Style & Naming Conventions

TypeScript uses two-space indentation, Next.js ESLint rules, kebab-case filenames, PascalCase components, and `use-*` hooks. Keep route-specific components beside `page.tsx` and shared primitives in `components/ui/`. Java uses Spotless/Google Java Format, PascalCase types, camelCase members, and feature packages. Keep controllers thin and business rules in services. Schema changes require a new Flyway migration; never edit an applied migration.

## Testing Guidelines

Backend tests use Spring Boot's test stack and end in `Tests.java`; run `./mvnw test`. Add focused coverage with each behavior change. The web app has no test script, so lint and production build are mandatory; document manual UI verification in the PR.

## Commit & Pull Request Guidelines

History favors short, imperative subjects such as `Add frontend auth screens (Phase 1e-i)`. Keep commits scoped to one coherent change and reference the roadmap phase when useful. PRs should explain intent, list verification commands, link issues or roadmap items, and include screenshots for UI changes. CI must pass before merge.

## Security & Configuration

Copy local examples rather than committing secrets. `apps/api/.env` holds JWT and OAuth credentials and must remain untracked; `apps/web/.env.local` configures the public API URL. Preserve default-deny API security and token-hashing rules described in `apps/api/CLAUDE.md`.
