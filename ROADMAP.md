# Orchestrate — Product and Engineering Roadmap

Orchestrate is a multi-tenant, human-in-the-loop workflow automation SaaS. The goal is not maximum feature count; it is a production-deployed system that demonstrates full-stack product delivery, reliable distributed processing, security, observability, and evidence-based performance improvements while keeping total monthly operating cost below **$40**.

## 1. Product thesis and success criteria

The primary demo is an operations workflow: a request enters through a form or webhook, rules and an optional AI step classify it, a person approves exceptional cases, an action runs, and every transition is auditable.

The project is portfolio-ready only when all of the following are true:

- A new user can complete the core journey without developer assistance.
- Workflow execution is durable, idempotent, retryable, tenant-isolated, and observable.
- CI enforces formatting, linting, automated tests, dependency scanning, and production builds.
- Production has HTTPS, real transactional email, backups, health checks, alerts, rollback instructions, and a tested restore procedure.
- Performance, reliability, AI quality, and user-time savings are measured from reproducible workloads. No resume claim is based on an estimate.
- Normal low-traffic operation, including a capped AI allowance, remains below $40/month.

### Scope rules

- Prefer one excellent end-to-end use case over many shallow node types or integrations.
- PostgreSQL is the system of record. Redis may accelerate rate limiting and coordination, but correctness must not depend on an evictable cache.
- Build a modular monolith and independently running worker before considering microservices or Kubernetes.
- Add infrastructure only when a measured constraint justifies it.
- Accessibility, security, testing, telemetry, and documentation are acceptance criteria in every phase, not a final cleanup exercise.

## 2. Current implementation baseline (July 2026)

### Phase 0 — Foundation — complete

- Next.js/TypeScript frontend and Java 21/Spring Boot API in a monorepo.
- PostgreSQL, Redis, frontend, and backend local services in Docker Compose.
- Pull-request CI for web lint/build and API formatting/package checks.
- Flyway-managed schema and environment-based application configuration.

### Phase 1 — Identity and organizations — complete

- Email/password signup, verification, reset, JWT access tokens, rotating opaque refresh tokens, and logout.
- Google and GitHub OAuth with verified-email account linking.
- Organizations, OWNER/ADMIN/MEMBER/APPROVER RBAC, membership management, last-owner protection, and invitations.
- Frontend screens for auth, organizations, members, roles, and invitations.

“Complete” records delivered scope; it does not imply production readiness. The audit found only a Spring context-load test, no frontend test framework, console-only email links, a known concurrent refresh-token race, no production profile/deployment, and no application telemetry. These become explicit Phase 2 work instead of being hidden in a late polish phase.

## 3. Target architecture

| Concern | Decision |
|---|---|
| Web | Next.js App Router, TypeScript, Tailwind, shadcn/ui, TanStack Query |
| API | Java 21, Spring Boot, Spring Security, Spring Data JPA |
| Persistence | Managed PostgreSQL; Flyway is schema authority |
| Execution | API writes jobs transactionally; a separate worker claims durable jobs from PostgreSQL using leases/locking |
| Redis | Upstash for distributed rate limits, short-lived cache, and optional signals—not primary job durability |
| AI | Provider adapter with structured outputs, validation, prompt versions, per-org limits, and cost accounting |
| Observability | OpenTelemetry/Micrometer structured logs, metrics, and traces sent to Grafana Cloud |
| Delivery | Containers, GitHub Actions, immutable image tags, automated migrations, health-gated deploy, rollback |
| Hosting | Cloudflare Pages/Workers for web; AWS Lightsail for API/worker; Neon and Upstash managed free tiers |

The API and worker remain one codebase with separate process roles. This preserves transactional consistency and a small operational footprint while still demonstrating asynchronous architecture.

## 4. Monthly operating budget

Budget assumptions are for one low-traffic production environment. Free tiers are useful but are not SLAs; limits, inactivity policies, and upgrade triggers must be documented and monitored.

| Item | Target | Monthly cap |
|---|---|---:|
| Frontend/CDN | Cloudflare free tier; Vercel Hobby only while strictly personal/non-commercial | $0 |
| API + worker | AWS Lightsail 2 GB Linux instance | $12 |
| PostgreSQL | Neon Free (0.5 GB); move to usage-based only when storage/compute data requires it | $0 |
| Redis | Upstash Free (256 MB, 500K commands/month) | $0 |
| Transactional email | Resend Free (3,000/month, 100/day) | $0 |
| Logs, metrics, traces, synthetic checks | Grafana Cloud Free | $0 |
| CI/CD | GitHub Actions public-repository allowance | $0 |
| Domain | Annual cost amortized | $1–2 |
| Claude API | Application-enforced monthly budget and provider-console limit | $10 |
| Contingency | Storage, DNS, backups, or small overages | $10 |
| **Maximum planned total** | | **$34** |

Set billing alerts at $20, $30, and $36. The application must reject or defer nonessential AI work before its $10 allowance is exhausted. Revisit prices quarterly and before deployment; provider pages are linked in [Sources](#10-sources-and-assumptions).

## 5. Delivery standards for every phase

Every feature PR must include acceptance criteria, tests at the lowest useful layer, tenant-authorization tests, migrations where required, structured telemetry, error/loading/empty UI states, accessibility checks, and documentation of operational impact.

The definition of done is:

1. Formatting, lint, unit, integration, and relevant E2E tests pass in CI.
2. API changes update an OpenAPI contract and include backward-compatibility consideration.
3. Database changes use additive Flyway migrations and include rollback/forward-recovery notes.
4. Logs contain correlation, organization, workflow, and execution identifiers but no secrets, raw tokens, or sensitive prompt content.
5. New asynchronous handlers are idempotent and have retry/dead-letter behavior tested.
6. The feature exposes the metrics needed to prove its user or engineering impact.

## 6. Phased roadmap

Time ranges are planning estimates, not deadlines. Each phase ends in a deployed, demonstrable increment.

### Phase 2 — Production foundation and quality gate (Weeks 9–13)

Close the gap between implemented Phase 1 behavior and a safe production baseline before building the workflow engine.

- Add Testcontainers PostgreSQL integration tests for auth, RBAC, invitations, Flyway migrations, and concurrent last-owner/invitation behavior.
- Repair/replace the generated Windows Maven wrapper path that currently fails before Maven starts; verify documented commands on Windows and Linux CI.
- Fix refresh-token concurrent replay with row locking or optimistic concurrency and add a deterministic regression test.
- Add Vitest + Testing Library for frontend logic/components and Playwright for the critical auth/org journey.
- Make CI run backend tests, frontend tests, dependency review, secret scanning, coverage reporting, Docker builds, and migration validation. Use coverage trends as a gap detector, not a vanity target; require high coverage on security and workflow state-transition code.
- Replace console email delivery with a provider interface and Resend implementation; retain a local log adapter. Add resend/idempotency handling without exposing account existence.
- Finalize same-site subdomains, Secure/HttpOnly cookie settings, CSRF protection, trusted-proxy headers, CORS, security headers, rate limits, request-size limits, and production secrets.
- Add RFC 7807-style error responses, request correlation IDs, structured JSON logs, Actuator readiness/liveness, OpenAPI docs, and architecture decision records.
- Create production Docker images that run non-root with pinned base images and health checks; remove hot-reload bind mounts from production.
- Deploy a thin production slice and write runbooks for deployment, rollback, database restore, key rotation, and incident response.

**Exit evidence:** CI exercises real PostgreSQL; critical E2E flows pass; zero unresolved high-severity dependency/security findings; backup restore and rollback are demonstrated; production synthetic checks run continuously.

### Phase 3 — Durable workflow engine (Weeks 14–20)

- Model workflows, immutable published versions, typed nodes/edges, executions, node attempts, and an append-only execution event history.
- Validate graphs on publish: one entry point, supported node configuration, valid references, and no unreachable nodes. Explicitly define loop policy.
- Implement a transactional outbox/job table and worker claim protocol with leases, bounded exponential backoff, dead-letter state, idempotency keys, optimistic versioning, and graceful shutdown/recovery.
- Begin with trigger, rule/decision, transform, and terminal nodes. Keep node handlers behind a small registry/port interface.
- Enforce organization scope in every query and mutation; add cross-tenant negative tests.
- Add REST APIs plus a form/JSON workflow editor, publish flow, run trigger, execution timeline, retry controls, and failure diagnostics.
- Record queue delay, node duration, end-to-end duration, retry count, throughput, and terminal outcome.

**Exit evidence:** killing the worker mid-run does not lose or duplicate externally visible work; the same idempotency key cannot create two runs; a reproducible k6 workload publishes p50/p95 latency, throughput, and recovery results under `docs/benchmarks/`.

### Phase 4 — Forms and product-quality workflow UX (Weeks 21–25)

- Add versioned public/internal form schemas, server-side validation, conditional fields, expiring submission links, spam/rate protection, and accessibility-complete rendering.
- Create a polished application shell, organization switcher, responsive workflow list/editor, autosaved drafts with conflict handling, run detail views, and actionable empty/error states.
- Add pagination/filtering at the API and database layers; prevent N+1 queries and verify indexes with `EXPLAIN (ANALYZE, BUFFERS)` on representative data.
- Add component tests, API contract tests, visual snapshots for stable primitives, and Playwright coverage of create → publish → submit → inspect.

**Exit evidence:** a first-time evaluator can build and run the reference workflow unaided; Lighthouse/accessibility findings are addressed; benchmark artifacts compare query and page performance before and after indexing/pagination.

### Phase 5 — Cost-controlled AI decision node (Weeks 26–30)

- Add an AI provider interface and one Claude structured-output node supporting classification, extraction, and summarization.
- Version prompts and schemas with workflow versions. Validate model output before routing; invalid/low-confidence output follows a deterministic fallback or human-review path.
- Enforce per-request token limits, timeouts, concurrency limits, per-organization quotas, a global monthly dollar cap, and a kill switch. Retry only safe transient failures.
- Minimize/redact sensitive input, define retention controls, and never log secrets or unrestricted prompt bodies.
- Store model/version, latency, token counts, estimated cost, validation outcome, confidence, and fallback reason.
- Build a checked-in, anonymized evaluation dataset and evaluation runner for accuracy, schema validity, latency, and cost. Compare at least one cheaper model/configuration before selecting the default.
- Surface AI rationale, confidence, cost, and human override in the execution UI without presenting model output as authoritative.

**Exit evidence:** an evaluation report states dataset size, accuracy/F1 where appropriate, schema-valid rate, p95 latency, and cost per 1,000 requests; budget-exhaustion and provider-outage tests prove deterministic degradation.

### Phase 6 — Human approvals, notifications, and audit (Weeks 31–36)

- Implement approval nodes that durably pause and resume executions; support approve, reject, request changes, reassignment, expiry, and escalation.
- Make decisions single-use and concurrency-safe. Record actor, timestamp, prior state, reason, and correlation ID in an append-only audit event.
- Send signed, expiring email notifications through an outbox with deduplication and delivery-status webhooks.
- Build an approvals inbox with filters, counts, accessible actions, optimistic UX with server reconciliation, and complete history.
- Measure automated processing time, waiting-on-human time, decision time, reminder count, and touch time.

**Exit evidence:** concurrent approval attempts yield one decision; notification retries do not duplicate messages; the reference workflow demonstrates pause/resume and a complete tamper-evident history.

### Phase 7 — Production integrations (Weeks 37–41)

- Add inbound and outbound webhooks with HMAC signatures, timestamp/replay protection, idempotency keys, timeouts, retry policies, circuit breaking, and delivery logs.
- Add one OAuth integration chosen for the reference story—Slack is preferred for notification and approval actions. Do not add a second until the first has contract and failure-path tests.
- Encrypt integration secrets at rest, support rotation/revocation, request minimum scopes, and prevent secret exposure in UI/logs.
- Provide an integration settings UI, test-delivery action, health state, and operator-facing failure recovery.

**Exit evidence:** contract tests cover provider/webhook behavior; chaos tests exercise timeouts, 429s, invalid signatures, and outages; delivery success rate and retry recovery are visible.

### Phase 8 — Analytics, search, and measurable business impact (Weeks 42–46)

- Build dashboards for runs, success/failure, queue delay, cycle time, approval wait, automation rate, AI cost, and human overrides.
- Use PostgreSQL full-text search for workflows, submissions, and audit metadata with tenant filters and cursor pagination. Defer vector search until a measured use case exists.
- Pre-aggregate only after query measurements justify it; document retention and deletion policies.
- Add a benchmark/demo harness that runs the same representative work manually and through Orchestrate, capturing elapsed time, active human time, error rate, and cost.
- Export sanitized benchmark results as JSON/CSV and render a public engineering case-study page explaining methodology and limitations.

**Exit evidence:** dashboards reconcile with source events; analytics queries meet documented p95 targets on seeded data; the case study produces defensible before/after results.

### Phase 9 — Portfolio release and operational proof (Weeks 47–50)

- Run threat modeling and an OWASP ASVS-inspired review; fix all high findings and document accepted lower risks.
- Run sustained load, soak, recovery, backup/restore, and dependency-failure tests against a production-like environment.
- Add SLOs and alerts for availability, API latency, workflow success, queue age, and AI spend; write one blameless game-day incident report with measured detection and recovery times.
- Add Terraform only for resources that are actually deployed. Keep secrets outside state and document bootstrap/recovery.
- Publish architecture diagrams, ADRs, API documentation, live demo, seeded demo account strategy, cost dashboard, benchmark methodology, and a concise case study.
- Perform a final accessibility, mobile, browser, privacy, license, and dependency review.

**Exit evidence:** 30 days of monitored low-traffic operation within budget; tested restore and rollback; published SLO report, load-test report, threat model, architecture overview, and quantified case study.

## 7. Measurement plan and resume evidence

Create `docs/benchmarks/README.md` in Phase 2 with environment details, dataset generator, commands, raw results, dates, commit SHA, and interpretation rules. Preserve baselines; never overwrite an old result.

| Outcome | Metric | Baseline | Comparison |
|---|---|---|---|
| Faster operations | End-to-end elapsed time and active human minutes | Manually complete a fixed batch | Run the same batch through the published workflow |
| Reliable execution | Completion rate, duplicate side effects, recovery time | Failure-injected initial engine | Final engine under the same fault schedule |
| Better performance | p50/p95/p99 latency and throughput | Before a named optimization | Same k6 scenario, data size, and host after change |
| Efficient AI | F1/accuracy, schema-valid rate, p95 latency, cost/1K | Rule-only or initial prompt/model | Selected prompt/model on frozen evaluation data |
| Lower approval delay | Median/p95 wait and reminder count | Email/manual process simulation | In-app inbox and escalation policy |
| Operational maturity | Detection time and recovery time | First game day | Later game day using alerts/runbooks |
| Cost control | Monthly cost and cost per completed run | First deployed month | Optimized month at comparable workload |

Resume bullets must be written only after measurements exist. Preferred shape: **action + engineering mechanism + measured outcome + workload/context**. Example template: “Designed an idempotent PostgreSQL-backed workflow worker with lease recovery, sustaining `[N]` runs/min at `[p95]` latency and recovering interrupted jobs in `[T]` without duplicate side effects during fault-injection tests.”

## 8. Explicit non-goals and decision checkpoints

- No Kubernetes, service mesh, Kafka, or microservice split without load/ownership evidence.
- No billing implementation, SAML, MFA, visual drag-and-drop canvas, RAG, or natural-language workflow generation before Phase 9. They do not strengthen the core proof enough to justify schedule and cost risk.
- Re-evaluate teams/workspaces only when a real scoping requirement cannot be represented by organizations.
- Re-evaluate PostgreSQL job claiming only after measurements show it is the bottleneck. If needed, compare Redis Streams or a managed queue with clear delivery semantics and cost.
- Re-evaluate the Neon free tier before 0.4 GB, Upstash before 400K commands/month, email before 80 messages/day, AI at 70% of its monthly allowance, and Lightsail when sustained memory exceeds 75% or CPU exceeds 70%.

## 9. Immediate next milestone

Start Phase 2 with a small sequence of reviewable PRs:

1. Establish Testcontainers and regression tests for existing auth/RBAC concurrency invariants.
2. Fix refresh-token concurrency and make CI run all tests.
3. Add frontend unit/E2E infrastructure and cover the Phase 1 happy path plus authorization failures.
4. Replace console email with a provider adapter and local fake.
5. Finalize the production domain/cookie/CSRF design and deploy the health/auth slice.
6. Add telemetry, runbooks, backup restore, and rollback evidence before Phase 3 schema design begins.

## 10. Sources and assumptions

Pricing and limits were checked on July 15, 2026. They can change and must be revalidated before purchasing or deploying.

- [AWS Lightsail pricing](https://aws.amazon.com/lightsail/pricing/)
- [Neon pricing](https://neon.com/pricing)
- [Upstash Redis pricing](https://upstash.com/pricing/redis)
- [Resend pricing](https://resend.com/pricing)
- [Cloudflare Pages pricing](https://developers.cloudflare.com/pages/functions/pricing/)
- [Vercel pricing and Hobby-plan scope](https://vercel.com/pricing)
- [Grafana Cloud pricing](https://grafana.com/pricing/)
- [GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions)
- [Anthropic pricing](https://docs.anthropic.com/en/docs/about-claude/pricing)
