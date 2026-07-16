# Repository Guidelines

## Project Structure & Module Organization

This Orchestrate monorepo has a Next.js 16 frontend in `apps/web/`: routes are in `src/app/`, shared UI in `src/components/`, hooks in `src/hooks/`, utilities in `src/lib/`, and assets in `public/`. The Java 21 Spring Boot backend is in `apps/api/`; feature packages are under `src/main/java/com/orchestrate/api/`, resources and Flyway migrations (`V*__description.sql`) under `src/main/resources/`, and tests under `src/test/java/`. Read the nearest nested guide before changing an app. `ROADMAP.md` defines phase scope; do not rewrite it without agreement.

## Reuse Before Building

Use this preference order:

1. Existing repository code or design-system component.
2. Language or platform standard library.
3. Capability already provided by an installed dependency or framework.
4. A compatible, mature, well-maintained dependency.
5. A custom implementation only when no suitable option exists or when a dependency would create more complexity or risk than the code it replaces.

Before adding a dependency, verify:

- compatibility with the current framework, runtime, and rendering environment;
- maintenance activity and ecosystem adoption;
- security advisories and transitive dependencies;
- license compatibility;
- bundle-size, runtime, and operational impact;
- accessibility and TypeScript support where applicable.

Do not create a parallel implementation when a suitable solution already exists, install a package merely because one exists, force-fit an incompatible package, add dependencies for speculative future use, or replace stable project code without a concrete benefit. When choosing custom code over an available dependency, briefly document why.

## Implementation Standards

- Prefer clear, conventional code over clever abstractions.
- Follow established project patterns before introducing new ones.
- Avoid premature abstraction, duplicated sources of truth, dead code, and unrelated refactoring.
- Preserve type safety; avoid `any`, unchecked casts, and suppressed warnings without justification.
- Validate input at system boundaries and return consistent, actionable errors.
- Handle failure paths explicitly; do not silently swallow exceptions.
- Keep secrets, credentials, tokens, and sensitive personal data out of source control and logs.
- Preserve default-deny authorization and least-privilege behavior.
- Consider accessibility, responsive behavior, loading, empty, error, and disabled states for UI changes.
- Optimize only when evidence identifies a meaningful performance problem.
- Write comments for non-obvious intent or constraints, not to restate the code.

## Frontend Requirements

- Use Next.js App Router, React, Tailwind CSS v4, and existing shadcn/Base UI primitives consistently.
- Read the relevant installed Next.js documentation under `node_modules/next/dist/docs/` before relying on version-sensitive behavior.
- Prefer Server Components unless client-side state or browser APIs require `"use client"`.
- Use semantic HTML and preserve keyboard and screen-reader support.
- Use two-space indentation, kebab-case filenames, PascalCase components, and `use-*` hook names.
- Do not introduce `tailwind.config.js`; theming is CSS-first through `@theme`.

Required verification:

```bash
cd apps/web
npm run lint
npm run build
```

Document manual UI verification when automated coverage is unavailable.

## Backend Requirements

- Keep controllers thin and business rules in services.
- Use request/response DTOs rather than exposing persistence entities as API contracts.
- Preserve validation, transaction boundaries, consistent error responses, and authorization checks.
- Schema changes require a new Flyway `V*__description.sql` migration. Never edit an applied migration.
- Preserve token hashing, secure cookie handling, generic authentication errors, and default-deny API security.
- Use Spotless/Google Java Format and name tests `*Tests.java`.
- Read `apps/api/AGENTS.md` and the linked architecture notes before changing security or domain invariants.

Required verification:

```bash
cd apps/api
./mvnw spotless:check test
```

Use `./mvnw spotless:apply` when formatting is required.

## Dependency and Configuration Changes

- Keep dependency changes intentional and update the appropriate lockfile.
- Do not modify generated files manually.
- Never commit `.env` files, credentials, private keys, or production secrets.
- Copy documented example configuration for local setup.
- Call out new environment variables, migrations, operational steps, or compatibility implications.

## Commit and Pull Request Guidelines

- Use short, imperative commit subjects and keep each commit scoped to one coherent change.
- Reference the roadmap phase when useful.
- PRs must explain intent, list verification commands, link related issues or roadmap items, and include screenshots for UI changes.
- Do not claim that checks passed unless they were run successfully.

## Security & Configuration

Copy local examples rather than committing secrets. `apps/api/.env` holds JWT and OAuth credentials and must remain untracked; `apps/web/.env.local` configures the public API URL. Preserve default-deny API security and token-hashing rules described in `apps/api/AGENTS.md`.
