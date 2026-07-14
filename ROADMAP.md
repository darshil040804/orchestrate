# Orchestrate — Build Roadmap

A phased plan for building Orchestrate as a portfolio-grade, resume-worthy full-stack application using Claude Code, staying within a $20–50/month budget over a 6–12 month timeline.

## Decisions locked in

- **Scope strategy:** Phased MVP → expand. Ship a small, genuinely production-quality core first (auth, orgs, workflow engine, one AI node type, one approval flow, a real dashboard). Everything else in the proposal is a later phase, not a day-one requirement.
- **Infra:** Vercel (frontend), Docker, a small AWS instance (backend), managed Postgres, GitHub Actions for CI/CD. Terraform comes once the deployment is stable. Kubernetes stays a stretch goal, not attempted until there's an actual scaling reason.
- **Claude Code:** You've used it a bit already, so this plan assumes basic familiarity and focuses on project-specific setup (CLAUDE.md, model choice, subagents, hooks, MCP).

---

## 1. Tech stack (final, for v1)

| Layer | Choice | Notes |
|---|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind, shadcn/ui, TanStack Query | Matches proposal exactly |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA | Matches proposal; strong resume signal (many "AI SaaS" portfolios are all-JS, Java backend differentiates you) |
| DB | PostgreSQL (managed — Neon or Supabase free tier initially, or RDS free tier for first 12 months) | |
| Cache/queue | Redis (Upstash free tier) | Used for workflow execution queue + rate limiting |
| AI | Claude API (Anthropic) directly from the backend | Gives you a real "AI engine," and you already have Anthropic API access |
| Auth | Spring Security + JWT, Google/GitHub OAuth | MFA and SSO stay stretch goals |
| Deploy | Frontend → Vercel; Backend → single small AWS instance (Lightsail or EC2 t4g.micro) in Docker; DB → managed Postgres | Terraform added in Phase 7 once this is stable |
| CI/CD | GitHub Actions: lint, test, build, deploy on merge to main | |
| Observability | Structured JSON logs from day 1; Prometheus + Grafana added in Phase 7 (self-hosted, free, on the same small instance or via a free-tier hosted option) | |

**Monthly cost estimate:** Vercel free tier ($0), small AWS instance (~$8–12), managed Postgres free/low tier ($0–15), Redis free tier ($0), domain (~$1/mo amortized), Claude API dev usage ($5–15 depending on how much you test AI nodes). Realistic total: **$15–40/mo**, leaving headroom.

---

## 2. Phased roadmap

Each phase ends with something demoable — this matters both for motivation and for resume/interview talking points ("I shipped X, then Y").

**Frontend scoping policy:** each phase includes a frontend slice only where it produces a surface a real user (not just an API client) interacts with directly; purely internal/backend machinery (e.g. the execution engine walking a workflow graph) doesn't get one. Phase 1's slices (1a–1d) were verified API-only via curl — Phase 1e below retroactively closes that gap before Phase 2 begins, and every phase from here on states its frontend scope explicitly rather than leaving it implicit.

### Phase 1 — Auth & Organizations (Weeks 3–6) — backend complete
- Email/password auth, email verification, password reset. ✅ (1a)
- Google + GitHub OAuth. ✅ (1b)
- Multi-tenancy: Organizations, RBAC (owner/admin/member/approver roles). ✅ (1c)
  - **Descoped:** Workspaces and Teams sub-org hierarchy — deliberately deferred at 1c kickoff to avoid building unused hierarchy into an MVP. Revisit only if a concrete feature need emerges; hard checkpoint to re-evaluate no later than Phase 5, when the data model gets reviewed holistically for reporting/scoping needs.
- Invitations flow. ✅ (1d)
- JWT issuance, refresh tokens, Spring Security config. ✅ (1a)

**Demo:** Sign up, verify email, create an org, invite a teammate, log in via GitHub OAuth. All verified via API to date — Phase 1e below makes this demoable through an actual UI.

### Phase 1e — Frontend for Auth & Organizations (Weeks 7–8)
Closes the gap above: Phase 1's backend has no UI yet. Split the same way 1a–1d were:
- 1e-i: auth screens — login, signup, email-verify landing, password-reset request/confirm, Google/GitHub OAuth buttons.
- 1e-ii: org screens — create org, org switcher/list, member roster, role management, remove member.
- 1e-iii: invitation screens — accept-invite page, admin invite-management panel.

**Demo:** A human can click through signup → email verification → org creation → inviting a teammate → GitHub OAuth login, with no curl involved.

### Phase 2 — Workflow Engine core (Weeks 9–15)
- Workflow data model: triggers, nodes (start simple — trigger, decision, notification, approval, AI), edges.
- Workflow versioning: publish = immutable; edits create new draft version.
- Execution engine: a worker process (can be a Spring `@Scheduled`/queue consumer to start) that walks a workflow graph, records state transitions. *(No dedicated frontend — internal machinery.)*
- Every execution logs timestamps, node history, retries, failures to Postgres.
- Frontend: a basic workflow builder (JSON/form-based config — visual drag-and-drop is a stretch goal, don't build it yet) and an execution history view.

**Demo:** Define a workflow with a trigger → decision → notification chain through the UI, run it, see full execution history rendered, not just logged.

### Phase 3 — AI Engine (Weeks 16–19)
- Integrate Claude API as an "AI node" type: classification, structured extraction, summarization to start.
- Log AI latency, token cost, and output per execution (feeds Engineering Metrics later).
- Validation layer after AI output, before it's allowed to affect routing — the "AI recommends, rules decide" architecture from the proposal, a strong interview talking point.
- Consider the posibility of using pnpm instead of npm.
- Frontend: a ticket-submission form (public), and the execution log view extended to surface AI reasoning/classification per step.

**Demo:** Submit a support ticket via a form → AI classifies category/priority → workflow routes based on that classification, with the AI's reasoning visible in the execution log UI.

### Phase 4 — Human Approval & Forms (Weeks 20–23)
- Approval nodes: pause execution, notify approver, resume on approve/reject/request-changes/delegate.
- Public + internal forms with validation and conditional fields (file upload can wait).
- Email notifications (start with just email — Slack is Phase 6).
- Frontend: a form builder/renderer, and an approvals inbox UI (approve/reject/request-changes/delegate actions).

**Demo:** A purchase-request workflow: form submission → AI drafts a summary → manager approval required above a threshold, actioned through the approvals inbox → execution resumes on approval.

### Phase 5 — Dashboard, Audit Trail, Analytics, Search (Weeks 24–28)
- Dashboard: active runs, pending approvals, failures, processing times.
- Audit trail: every meaningful event recorded and queryable.
- Analytics: completion time, bottlenecks, approval delays, automation %.
- Basic search across workflows/submissions/audit logs (Postgres full-text search — skip vector search for now).
- Frontend: the dashboard, audit trail viewer, and analytics/search UI *are* this phase's deliverable, not an add-on.
- **Checkpoint:** re-evaluate the deferred Workspaces/Teams decision here (see Phase 1 note) while reviewing the data model holistically.

**Demo:** A real dashboard you'd screenshot for a portfolio site — this is your "looks like a real product" milestone.

### Phase 6 — Integrations, phase two (Weeks 29–34)
- Slack notifications + Slack approval actions.
- Webhooks (inbound trigger + outbound action) done properly, with retries and signature verification.
- Pick one more: Google Sheets or Gmail, whichever tells a better story for your target use case (e.g., invoice processing → Sheets export).
- Frontend: an integrations settings page (connect Slack, configure webhooks/the chosen integration).

**Demo:** A workflow that notifies via Slack and lets someone approve from a Slack button.

### Phase 7 — Hardening & SaaS polish (Weeks 35–42+)
- Observability: Prometheus + Grafana dashboards for API latency, workflow duration, failure rate.
- Security pass: use the `security-review` skill/process against your own repo, fix findings, document the process (great resume bullet).
- Load testing (k6 or similar) against the workflow execution path; publish numbers.
- Terraform for the AWS resources you're already running (infra-as-code retrofit).
- SaaS scaffolding: subscription plans (Stripe test mode), usage tracking, API keys, minimal public API docs.
- Test coverage push: unit + integration + a handful of E2E tests (Playwright) — include the org last-owner concurrency regression test from Phase 1c here.
- Production-style build instead of Hot-Reload Build.
- Frontend: billing/subscription page, API key management page.

**Demo:** A polished, monitored, tested, documented product with a public API — this is your "portfolio-ready" state.

### Stretch (only if time remains)
Pick 1–2 for differentiation, don't chase all of them: visual drag-and-drop workflow builder, semantic search/RAG over documents, natural-language workflow generation, SSO/SAML.

---

## 3. Claude Code: model selection

| Task type | Model | Why |
|---|---|---|
| Architecture decisions, tricky debugging, security review, designing the workflow engine's core abstractions | **Opus** | Worth the cost for the handful of decisions that are expensive to get wrong |
| Day-to-day feature implementation, most of the actual coding | **Sonnet** | Best cost/quality balance; this should be ~80% of your usage |
| Boilerplate (CRUD scaffolds, DTOs, simple test cases, repetitive refactors), delegated via subagents | **Haiku** | Cheap, fast, fine for mechanical work |

Practical rule: default session on Sonnet. Switch to Opus only when you're about to make a decision that's hard to undo (schema design, execution engine architecture, security posture). Push repetitive/mechanical sub-tasks to Haiku-backed subagents so they don't eat Sonnet/Opus budget.

---

## 4. Claude Code project setup

### CLAUDE.md (repo root)
This is the single highest-leverage thing for reducing token usage — it means you stop re-explaining context every session. Include:
- One-paragraph project description + current phase you're in.
- Tech stack list (from Section 1).
- Repo structure (`apps/web`, `apps/api`, etc.).
- Conventions: naming, commit message format, where tests live, how to run the app locally.
- "Do not" list: e.g., don't introduce new dependencies without asking, don't touch `apps/api/src/main/resources/application-prod.yml`.
- Link to the current phase's scope (you can keep a `ROADMAP.md` — this file — and reference it).

Add nested `CLAUDE.md` files in `apps/web` and `apps/api` for stack-specific conventions once those directories have real content — this keeps root CLAUDE.md short and lets Claude Code load only what's relevant.

Use the `init` skill to bootstrap this in your repo rather than writing it from scratch.

### Subagents
Define a few narrow subagents rather than one do-everything agent:
- **test-writer** — given a file/feature, writes unit tests only. Cheap model (Haiku/Sonnet).
- **code-reviewer** — reviews a diff before you commit; independent from the implementing session so it isn't anchored on the same reasoning.
- **security-review** (already available to you) — run this periodically, especially before Phase 7 hardening, and after any auth/workflow-execution changes.

Keep subagents read-only or narrowly scoped where possible — it's easier to trust their output.

### Hooks
Useful ones for this project:
- **PostToolUse hook** running `mvn -q -pl apps/api test-compile` or a lint command after edits to backend files, so mistakes surface immediately instead of at commit time.
- **PreCommit-style hook** blocking commits if `npm run lint` / `mvn checkstyle:check` fails.
- A hook that reminds you (or blocks) when editing anything under `apps/api/src/main/resources/application-prod.yml` or secrets files.

Start with just one (lint-after-edit) — add more only when you feel the pain they'd solve.

### MCP servers
Don't set these all up on day one. Add as the relevant phase starts:
- **Postgres MCP** — once the schema exists (Phase 1+), lets Claude Code inspect your actual schema instead of guessing.
- **GitHub MCP** — useful once you're managing issues/PRs as part of the workflow (optional, nice-to-have).
- **Playwright/browser MCP** — add in Phase 7 when you're writing E2E tests.

Do not add a dozen MCP servers speculatively — each one adds to what Claude Code has to reason about and can slow things down for no benefit until you're actually using it.

### Memory
Claude Code's memory is really just CLAUDE.md + your own discipline about keeping it current. Practical habit: at the end of each phase, spend 10 minutes updating CLAUDE.md with anything that changed (new conventions, new "don't touch this" items, new run commands). Treat it like updating documentation, because it is.

---

## 5. Prompting efficiently (reduce token usage)

- **Reference files, don't paste them.** Say "look at `apps/api/src/main/java/.../WorkflowService.java`" instead of pasting the file into your prompt — Claude Code will read what it needs.
- **Scope each session to one feature/ticket.** Don't ask for Phase 2 and Phase 3 in the same conversation. Use `/clear` (or start a fresh session) between unrelated tasks so old context isn't dragging along.
- **Use plan mode for anything non-trivial.** For new features (a new node type, a new integration), ask Claude Code to propose a plan first, review it, then have it execute. This avoids expensive rework from a wrong assumption made three steps in.
- **Be specific about "done."** State the acceptance criteria in the prompt ("add an endpoint that does X, returns Y, add a test for Z") rather than "add invoice processing" — vague asks cause exploratory (expensive) reasoning.
- **Delegate mechanical work to subagents/Haiku**, keep your main Sonnet/Opus session for design and integration work.
- **Avoid "read the whole codebase" asks.** Point at specific directories or files relevant to the task.
- **Use `/compact` when a long session's context is mostly resolved decisions** rather than letting it grow unbounded.

---