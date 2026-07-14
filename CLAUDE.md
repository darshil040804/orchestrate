# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Orchestrate — AI-assisted workflow automation SaaS for building reliable, human-in-the-loop operational workflows (trigger → AI classification → human approval → action, with full audit history). Portfolio-grade project, built incrementally, budget-constrained ($20–50/month infra).

## Current status

**Phase 1 in progress**.

## Planned tech stack

| Layer | Choice |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind, shadcn/ui, TanStack Query |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| DB | PostgreSQL (managed) |
| Cache/queue | Redis |
| AI | Claude API, called directly from the backend as an "AI node" workflow type — AI recommends, rules decide (validation layer gates AI output before it affects routing) |
| Local dev | Docker Compose (Postgres, Redis, backend, frontend) |
| CI/CD | GitHub Actions: lint, test, build on PR; deploy on merge to main |

## Planned monorepo layout

```
apps/web/       Next.js frontend
apps/api/       Spring Boot backend
packages/       shared types / OpenAPI spec
```

Add nested `CLAUDE.md` files in `apps/web` and `apps/api` once those directories have real content, so stack-specific conventions don't bloat this root file.

## Don't touch without asking

- `apps/api/src/main/resources/application-prod.yml` (once it exists) and any secrets/credentials files.
- Don't introduce new dependencies or infra (Terraform, Kubernetes, extra MCP servers) speculatively — the roadmap defers these until there's a concrete need.
- Don't pull work forward from a later phase without confirming with the user.

# How to run this

# Don't touch without asking

- don't modify ROADMAP.md's phase scope/timeline itself without confirming