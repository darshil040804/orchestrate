# Orchestrate Repository Guide

## Instruction Scope

- This is the canonical shared guide for all coding agents. Claude Code loads it through `CLAUDE.md`.
- Before modifying a file, read every `AGENTS.md` from the repository root through that file's directory. The closest guide takes precedence.
- Follow the user request and the current phase in `ROADMAP.md`. Do not change roadmap scope or pull later-phase work forward without explicit agreement.
- Make the smallest coherent change that solves the requested problem.
- Preserve unrelated work, public behavior, and existing architectural decisions unless the task requires changing them.
- Do not commit, push, open a PR, or perform destructive Git operations unless explicitly requested.

## Repository Structure

- `apps/web/`: Next.js 16 App Router frontend.
- `apps/api/`: Java 21 Spring Boot backend.
- `packages/`: shared contracts and specifications, when present.
- Keep route-specific frontend code beside its route and reusable UI under `src/components/`.
- Keep backend features under `com.orchestrate.api`; controllers handle transport concerns and services own business rules.

## Engineering Workflow

1. Inspect the relevant code, tests, configuration, and nearby patterns before editing.
2. Search the repository for an existing implementation, component, hook, utility, service, or dependency that already solves the problem.
3. Confirm version-specific behavior using installed-package or official documentation.
4. Implement the smallest maintainable solution consistent with the existing architecture.
5. Add or update focused tests for changed behavior and important failure cases.
6. Run the narrowest relevant checks first, followed by the required app checks.
7. Report what changed, what was verified, and any remaining risk.

Do not guess when the repository, dependency source, or official documentation can establish the answer.

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

## Token and Communication Efficiency

- Use targeted searches and read only relevant files or line ranges.
- Exclude generated and vendor directories such as `node_modules`, `.next`, and `target`.
- Do not repeatedly scan or reread unchanged files.
- Avoid speculative implementation plans for routine changes.
- Ask questions only when missing information would materially change the solution.
- Keep progress updates and final summaries concise.
- Do not paste large files or command output when a short summary is sufficient.
- Do not create extra documentation, abstractions, tests, or comments unrelated to the requested behavior.
- Do not delegate to additional agents unless explicitly requested.
