# ClaimGuard

ClaimGuard is an insurance claim intake system for hospital bills. A claimant or hospital uploads
claim documents (PDF or image); a vision-language model reads them into structured fields; a
fraud/consistency engine checks the documents and the claim for duplicate submissions, reused
invoice numbers, tampering artefacts, and clinically implausible figures; a decision engine either
auto-approves the claim or routes it to a human reviewer with the specific reasons why; and every
step — creation, upload, risk assessment, decision, reviewer action — is written to a
tamper-evident, hash-chained audit log that can be independently re-verified.

For a guided end-to-end walkthrough (including what to say and what should happen on screen), see
[`samples/DEMO.md`](samples/DEMO.md). Synthetic sample claim documents used for that demo and for
manual testing live in [`samples/`](samples/README.md).

## Architecture

```
Next.js console (Zitadel sign-in) ──Bearer JWT──▶ Spring Boot API ──▶ Neon Postgres
                                                        │
                                                        ├──▶ Cloudflare R2 (document storage)
                                                        │
                                                        ├──▶ Cloudflare Workers AI / Groq
                                                        │    (vision-language document reader,
                                                        │     text embeddings for semantic dupes)
                                                        │
                                                        └──▶ HAPI FHIR (R4 bundle build/validate)
```

- **Backend** — Spring Boot (Java 17) on Neon (managed Postgres), Flyway-migrated. Documents are
  stored in Cloudflare R2 (S3-compatible) behind an abstraction (`StorageService`) with an
  unconfigured no-op fallback when R2 isn't set up. Document reading (turning a PDF/image into
  structured claim fields) is done by a VLM chat-completions call — Cloudflare Workers AI and/or
  Groq, tried in order, PDFs rasterized to an image first via PDFBox. A fraud/consistency engine
  (`com.claimguard.fraud`) computes SHA-256 and perceptual-image hashes, PDF/EXIF forensic
  metadata, semantic-duplicate embeddings (Cloudflare embeddings + pgvector-style lookup), and
  cross-checks dates/totals/diagnosis against a small clinical reference table, then feeds a
  weighted signal list into a decision engine that auto-approves or flags for review with named
  reasons. Every mutating action is recorded to an append-only, SHA-256 hash-chained audit log
  (`com.claimguard.audit`) that a `/api/audit/verify` endpoint re-derives and checks end to end.
- **Frontend** — Next.js (App Router) reviewer console. Sign-in is NextAuth with the Zitadel
  provider; every backend call from the server is proxied through `backendFetch`, which attaches
  the signed-in user's access token as a `Bearer` JWT and refreshes it before it expires.
- **Auth** — Zitadel (OIDC). The backend is an OAuth2 resource server that validates the token as a
  JWT — the Zitadel application's **Token Settings → Auth Token Type must be JWT**, not opaque, or
  every API call will 401 after a successful sign-in.

## Repository layout

```
backend/    Spring Boot API (Java 17, Maven)
  src/main/java/com/claimguard/
    claim/        claim + document CRUD, upload/download through R2
    extraction/   document reading (VLM calls), PDF rasterization, extraction queue
    fraud/        duplicate/forensic/consistency signal detection, risk scoring
    decision/     auto-approve vs. needs-review, reviewer approve/hold/escalate
    audit/        hash-chained audit log, CSV export, chain verification
    fhir/         FHIR R4 claim bundle building (HAPI FHIR) + a stub NHCX gateway
    metrics/      dashboard metrics (straight-through rate, leakage, SLA, etc.)
    settings/     read-only settings snapshot for the frontend
    storage/      R2 storage abstraction (+ unconfigured fallback)
    notify/       transactional email (Resend) (+ unconfigured fallback)
    analytics/    product analytics (PostHog) (+ unconfigured fallback)
    samples/      SampleGenerator — synthetic demo claim documents (see samples/README.md)
    config/       Spring Security, CORS, tech-disclosure header filter
    support/      shared parsing/formatting helpers, .env loader
  src/main/resources/db/migration/   Flyway SQL migrations (V1..V5)
frontend/   Next.js reviewer console (App Router, Tailwind)
  app/            marketing landing (/), features (/features), sign-in (/signin)
  app/dashboard/  metrics dashboard, claims list/detail, review queue, audit trail, settings
  middleware.ts   mirrors the backend's runtime-derived stack headers onto app responses
  lib/            backend proxy fetch, auth (NextAuth + Zitadel), formatting helpers
  components/     claim forms, risk/decision panels, review actions, audit UI, marketing sections
samples/    Synthetic sample claim documents + README.md + DEMO.md (this is the demo fixture set)
```

## Running it locally

### Backend

Prerequisites: JDK 17, a Postgres database (Neon or local), and a `backend/.env` file (the app
loads `.env` itself on startup — see `com.claimguard.support.Dotenv` — no `export` needed).

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080` (`PORT` env var to change it). `GET /api/ping` is unauthenticated
and always available, so it's the quickest liveness check.

Flyway runs the migrations in `src/main/resources/db/migration` automatically on startup.

### Frontend

Prerequisites: Node.js, and a `frontend/.env` file.

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:3000`.

### Regenerating sample data

```bash
cd backend
./mvnw -q compile exec:java -Dexec.mainClass=com.claimguard.samples.SampleGenerator
```

Writes synthetic hospital bill PDFs/PNGs (clean and deliberately fraudulent) to `../samples`. See
[`samples/README.md`](samples/README.md) for what each file is built to trigger.

## Environment variables

### Backend (`backend/.env` or process environment)

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DATABASE_URL` | yes | — | Postgres connection string (`postgres://user:pass@host/db[?...]`); parsed into `spring.datasource.*` by `Dotenv`. |
| `OIDC_ISSUER_URI` | yes, to enforce auth | — | Zitadel issuer URL. Mapped to `spring.security.oauth2.resourceserver.jwt.issuer-uri`. If unset, the API runs with **no auth at all** (every request permitted) — fine for local hacking, not for anything else. |
| `APP_EXPOSE_TECH` | no | unset (off) | If `true`, adds `Server`/`X-Powered-By`/`X-Runtime` response headers disclosing the Java/Spring/Tomcat versions. |
| `app.cors.allowed-origins` | no | `http://localhost:3000` | Spring property (env var form `APP_CORS_ALLOWED_ORIGINS`), comma-separated origins allowed to call the API. |
| `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | no (all four together, or none) | — | Cloudflare R2 (S3-compatible) document storage. Without these, uploads use an in-memory/no-op `UnconfiguredStorageService`. |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | no | — | Enables Cloudflare Workers AI as a document reader and as the embeddings provider for semantic-duplicate detection. |
| `CLOUDFLARE_MODEL` | no | `@cf/meta/llama-4-scout-17b-16e-instruct` | VLM used to read documents via Cloudflare. |
| `CLOUDFLARE_BASE_URL` | no | `https://api.cloudflare.com` | Override for testing against a proxy/mock. |
| `CLOUDFLARE_MAX_TOKENS` | no | `4000` | Max response tokens for the Cloudflare document reader call. |
| `EMBEDDING_MODEL` | no | `@cf/baai/bge-base-en-v1.5` | Cloudflare embedding model for semantic duplicate detection. |
| `EMBEDDING_DIMENSIONS` | no | `768` | Expected embedding vector size. |
| `EMBEDDING_TIMEOUT_SECONDS` | no | `60` | HTTP timeout for embedding calls. |
| `GROQ_API_KEY` | no | — | Enables Groq as a (fallback) document reader. |
| `GROQ_MODEL` | no | `qwen/qwen3.6-27b` | VLM used to read documents via Groq. |
| `GROQ_BASE_URL` | no | `https://api.groq.com` | Override for testing against a proxy/mock. |
| `GROQ_MAX_TOKENS` | no | `4500` | Max response tokens for the Groq document reader call. |
| `READER_TIMEOUT_SECONDS` | no | `120` | HTTP timeout for document reader calls. If neither Cloudflare nor Groq credentials are set, documents are never read (`UnconfiguredDocumentReader`) and claims stay stuck pre-extraction. |
| `PDF_RENDER_DPI` | no | `150` | DPI used to rasterize PDFs before sending them to a VLM / hashing them. |
| `PDF_MAX_PAGES` | no | `2` | Max pages rasterized per document. |
| `PDF_MAX_WIDTH` | no | `1600` | Max rasterized image width in pixels. |
| `PDF_MAX_PIXELS` | no | `1800000` | Max rasterized image area in pixels. |
| `PDF_JPEG_QUALITY` | no | `0.72` | JPEG quality used for the rasterized image sent to the VLM. |
| `EXTRACTION_WORKERS` | no | `4` | Thread pool size for the extraction queue. |
| `EXTRACTION_QUEUE_CAPACITY` | no | `200` | Extraction task queue capacity. |
| `EXTRACTION_LEASE_SECONDS` | no | `900` | How long a worker owns an extraction job. Once the lease expires the sweeper may hand the job to another worker, so this must exceed the slowest realistic read. |
| `EXTRACTION_MAX_ATTEMPTS` | no | `3` | How many times a failed extraction is retried before it is left failed. |
| `EXTRACTION_RETRY_BACKOFF_SECONDS` | no | `60` | Base delay before retrying a failed extraction; doubles per attempt. |
| `EXTRACTION_PENDING_GRACE_SECONDS` | no | `300` | How long a job may sit `PENDING` before the sweeper assumes its submission was lost and requeues it. |
| `EXTRACTION_SWEEP_INTERVAL_MS` | no | `60000` | How often the sweeper looks for stalled and retryable extractions. |
| `EXTRACTION_SWEEP_INITIAL_DELAY_MS` | no | `60000` | Delay before the first sweep after boot. |
| `UPLOAD_MAX_BYTES` | no | `26214400` | Largest accepted document. Keep at or below `spring.servlet.multipart.max-file-size`. |
| `UPLOAD_BURST` | no | `10` | Uploads a single subject may make back-to-back before the limiter refuses. |
| `UPLOAD_PER_MINUTE` | no | `30` | Sustained upload rate per subject. Bounds paid VLM spend from one caller. |
| `CLAIM_SCOPE` | no | `org` | Which claims a caller may see: `org` (same tenant), `user` (own claims only) or `none` (no scoping). Ignored when `OIDC_ISSUER_URI` is unset, since there is no caller to scope by. |
| `TENANT_CLAIM` | no | `urn:zitadel:iam:user:resourceowner:id` | JWT claim read as the caller's organisation for `CLAIM_SCOPE=org`. |
| `ROLES_CLAIM` | no | `urn:zitadel:iam:org:project:roles` | JWT claim read as the caller's roles. |
| `REVIEWER_ROLES` | no | — (empty: everyone may review) | Comma-separated roles allowed to record review decisions and delete claims. |
| `AUTO_APPROVE_MAX_SCORE` | no | `24` | Risk score (0–100) at or below which a claim with no HIGH/CRITICAL signal auto-approves. |
| `DECISION_SLA_HOURS` | no | `24` | SLA window used by the metrics endpoint to report claims open past SLA. |
| `RESEND_API_KEY` | no | — | Enables transactional email via Resend. Without it, notifications are a no-op (`UnconfiguredNotifier`). |
| `NOTIFY_TO` | no | — | Comma-separated recipient list; required alongside `RESEND_API_KEY` for notifications to actually send. |
| `NOTIFY_FROM` | no | `ClaimGuard <onboarding@resend.dev>` | From address for outgoing email. |
| `RESEND_BASE_URL` | no | `https://api.resend.com` | Override for testing against a proxy/mock. |
| `NOTIFY_TIMEOUT_SECONDS` | no | `20` | HTTP timeout for the Resend call. |
| `POSTHOG_API_KEY` | no | — | Enables backend product-analytics events. Without it, analytics calls are a no-op. |
| `POSTHOG_HOST` | no | `https://eu.i.posthog.com` | PostHog ingestion host. |
| `POSTHOG_TIMEOUT_SECONDS` | no | `10` | HTTP timeout for PostHog calls. |
| `NHCX_PARTICIPANT_CODE` | no | `claimguard.demo@hcx` | Participant code used by the stub NHCX gateway when submitting a FHIR claim bundle. |
| `PORT` | no | `8080` | HTTP port Spring Boot listens on. |
| `SENTRY_DSN` | no | — | Enables Sentry error reporting. Empty means Sentry is inert. `send-default-pii` is forced false because this service handles claim data. |
| `SENTRY_ENVIRONMENT` | no | `local` | Environment tag on Sentry events. |
| `SENTRY_TRACES_SAMPLE_RATE` | no | `0.0` | Performance-trace sampling rate. |
| `API_DOCS_ENABLED` | no | `false` | Serves `/docs`, `/redoc` and `/v3/api-docs`. Off by default so the endpoint surface isn't published; set `true` locally to browse the API. |
| `STACK_EXTRA_POWERED_BY` | no | — | Extra tokens appended to the derived `X-Powered-By` header. Everything else in that header is read from the running JVM. |

### Frontend (`frontend/.env`)

| Variable | Required | Purpose |
|---|---|---|
| `AUTH_SECRET` | yes | NextAuth session encryption secret. |
| `AUTH_ZITADEL_ID`, `AUTH_ZITADEL_SECRET`, `AUTH_ZITADEL_ISSUER` | yes | Zitadel OIDC application credentials/issuer used by the NextAuth Zitadel provider (also used to refresh access tokens). |
| `NEXT_PUBLIC_API_URL` | yes | Backend base URL, used client-side and as the fallback for server-side backend calls. |
| `BACKEND_URL` | no | Server-side-only override for the backend base URL (falls back to `NEXT_PUBLIC_API_URL`). Use this when the server and browser reach the backend at different addresses. |
| `NEXT_PUBLIC_POSTHOG_HOST`, `NEXT_PUBLIC_POSTHOG_KEY` | no | Client-side PostHog product analytics. |
| `NEXT_PUBLIC_TAWK_PROPERTY_ID`, `NEXT_PUBLIC_TAWK_WIDGET_ID` | no | Tawk.to support chat widget. |

## API surface

All routes below are under the Spring Boot backend and, when `OIDC_ISSUER_URI` is set, require a
`Bearer` JWT except `GET /api/ping` and `GET /actuator/health/**`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/ping` | Unauthenticated liveness check. |
| `GET` | `/api/me` | Current authenticated principal (subject + claims), or `{"authenticated": false}`. |
| `GET` | `/api/settings` | Read-only settings snapshot for the frontend. |
| `POST` | `/api/claims` | Create a claim (reference/claimant/note; reference auto-generated if blank). |
| `GET` | `/api/claims` | List claims (summary view), paged: `?page=` (default 0), `?size=` (default 50, max 200). Returns `{items, page, size, totalItems, totalPages}`. |
| `GET` | `/api/claims/queue/review` | List claims currently needing review, paged like `/api/claims`. |
| `GET` | `/api/claims/{id}` | Claim detail — documents, extraction, risk, decision. |
| `GET` | `/api/claims/{id}/status` | Cheap status snapshot (`status`, `updatedAt`, `documentCount`, `processing`) for polling while documents are read. |
| `PUT` | `/api/claims/{id}` | Update a claim's editable fields/status. |
| `DELETE` | `/api/claims/{id}` | Delete a claim. |
| `POST` | `/api/claims/{id}/documents` | Upload a document (multipart `file`; optional `X-Device-Fingerprint` header for the shared-device fraud signal). |
| `DELETE` | `/api/claims/{claimId}/documents/{documentId}` | Delete a document. |
| `GET` | `/api/claims/{claimId}/documents/{documentId}/content` | Stream the original document bytes (proxied through the backend, not a direct R2 link). |
| `GET` | `/api/claims/{claimId}/documents/{documentId}/extraction` | Get the extraction result for a document. |
| `POST` | `/api/claims/{claimId}/documents/{documentId}/extraction` | Re-run extraction for a document. |
| `PATCH` | `/api/claims/{claimId}/documents/{documentId}/extraction` | Apply manual field corrections to an extraction. |
| `GET` | `/api/claims/{claimId}/decision` | Get the current decision (auto or reviewer) for a claim. |
| `POST` | `/api/claims/{claimId}/review` | Record a reviewer action: approve, hold (flag), or escalate. |
| `GET` | `/api/claims/{claimId}/fhir` | Build and return a FHIR R4 claim bundle (`application/fhir+json`). |
| `POST` | `/api/claims/{claimId}/nhcx` | Submit the claim's FHIR bundle to the (stub) NHCX gateway. |
| `GET` | `/api/audit` | Recent audit events (`?limit=`, default 100, max 500). |
| `GET` | `/api/audit/page` | Audit events paged: `?page=`, `?size=` (default 100, max 500). |
| `GET` | `/api/audit/claims/{claimId}` | Audit events for one claim. |
| `GET` | `/api/audit/verify` | Verify the audit hash chain from the last stored checkpoint and advance it. |
| `GET` | `/api/audit/export` | Export the audit log as CSV. |
| `GET` | `/api/metrics` | Dashboard metrics: straight-through rate, leakage caught, SLA, risk band and top-signal breakdowns. |
| `GET` | `/api/stack` | Unauthenticated. Runtime-derived stack (Tomcat, Servlet, Spring Boot, Spring, Java versions) read from the live JVM. The frontend middleware mirrors these onto its own responses so one domain advertises the whole stack. |

### Browsable API documentation

Generated by springdoc from the running controllers, so it cannot drift from the code. All three are
unauthenticated, and all three can be switched off with `API_DOCS_ENABLED=false`.

| Path | What it serves |
|---|---|
| `/docs` | Swagger UI (bundled locally, works offline). `/swagger-ui.html` still redirects here. |
| `/redoc` | ReDoc rendering the same spec (loads its bundle from a CDN, so it needs internet). |
| `/v3/api-docs` | The raw OpenAPI 3.1 document. |

## What's not set up yet

Being upfront about gaps rather than implying they exist:

- **No deploy pipeline.** CI builds and tests both trees, but nothing is deployed. Railway (backend)
  and Vercel (frontend) are the intended targets per `STACK.md`; deploy jobs are deliberately not
  wired up yet.
- **No per-user or per-tenant scoping.** Any signed-in user can see and act on every claim. This is
  the one gap that must close before a pilot, and it needs a product decision first — scope by user,
  or by Zitadel organisation?
- **Extraction is not durable.** Jobs run on an in-process thread pool behind the `ExtractionQueue`
  interface. `ExtractionRecovery` re-queues unfinished rows on boot, but this means **only one
  backend instance may run at a time** — two would double-process documents. Upstash QStash is the
  intended fix.
- **AI-generated image detection never fires.** The `AiImageDetector` interface exists but no free
  provider is wired in.
- **NHCX submission is a stub.** Bundles are real FHIR R4 and NHCX-shaped, but nothing is
  transmitted; no exchange endpoint is configured.
