# Deploying ClaimGuard

Backend → **Render** (container pulled from **GHCR**). Console → **Vercel**.
Secrets → **Doppler**, synced into both platforms by their native integrations.
Database stays on **Neon**.

You are doing the first deploy by hand. This is the full list of what to
configure, in the order that avoids chicken-and-egg problems. CI takes over at
the end (§8).

---

## 0. Prerequisite — the repo does not currently build

`backend/.gitignore` had `storage/` on line 43, intended for a local scratch
directory. Git treats an unanchored pattern as matching at any depth, so it also
matched the Java package `backend/src/main/java/com/claimguard/storage/` — and
those four files (`StorageService`, `R2StorageService`,
`UnconfiguredStorageService`, `StorageConfig`) were never committed.

Anything building from a clean clone — CI, and the GHCR image build — fails with
`package com.claimguard.storage does not exist`. It works on your machine only
because the files are there untracked. This is why the `backend` CI job has been
red since 31 July.

Fixed by anchoring the pattern to `/storage/`. You still need to commit the
package:

```bash
git add backend/.gitignore backend/src/main/java/com/claimguard/storage/
git status --short backend/src
```

Verify it is really in the repo before pushing an image:

```bash
git ls-files backend/src/main/java/com/claimguard/storage/   # must list 4 files
```

## 0b. Order of operations

The two platforms each need the other's URL, so do it in this order:

1. Neon — confirm pgvector and grab `DATABASE_URL`
2. Doppler — create the project and fill in every secret
3. GHCR — build and push the backend image
4. Render — create the service (gives you the API URL)
5. Vercel — create the project using that API URL (gives you the console URL)
6. Zitadel — add the console URL as a redirect URI
7. Render — set `APP_CORS_ALLOWED_ORIGINS` to the console URL
8. Verify, then hand deploys to CI

---

## 1. Neon

Nothing to create if you are reusing the current database. Confirm two things:

```sql
-- Flyway V4 runs this itself, but the role must be allowed to:
create extension if not exists vector;
select extversion from pg_extension where extname = 'vector';
```

Flyway migrates `V1..V6` automatically at boot (`baseline-on-migrate: true`).
Note the region — pick the matching Render region in §4.

`DATABASE_URL` must be the `postgres://user:pass@host/db?sslmode=require` form.
`Dotenv.java` parses it into `spring.datasource.*` itself; do **not** hand
Render a `jdbc:` URL.

---

## 2. Doppler

Create project **`claimguard`** with configs `dev` / `stg` / `prd`.

Two separate secret sets — the backend and the console share almost nothing.
Cleanest layout is **two projects** (`claimguard-api`, `claimguard-web`) or one
project with two configs per environment. Either works; the integrations attach
per-config.

### 2a. Backend secrets (`claimguard-api` → `prd`)

**Required — the app is broken or wide open without these:**

| Secret | Notes |
|---|---|
| `DATABASE_URL` | From Neon. |
| `OIDC_ISSUER_URI` | Your Zitadel issuer. **If this is unset the API permits every request with no auth at all** (`SecurityConfig.java:28`). This is the single most important production value. |

**Required for the product to actually function:**

| Secret | Notes |
|---|---|
| `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | All four together. Without them uploads go to a no-op store and documents vanish. |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | Primary document reader **and** the embeddings provider for semantic duplicates. |
| `GROQ_API_KEY` | Fallback reader. Without any reader, claims stick before extraction forever. |

**Optional (each degrades to a silent no-op):**

| Secret | Effect when absent |
|---|---|
| `SENTRY_DSN` | No error reporting. |
| `RESEND_API_KEY`, `NOTIFY_TO`, `NOTIFY_FROM` | No email on flagged claims. |
| `POSTHOG_API_KEY`, `POSTHOG_HOST` | No backend analytics. |
| `NHCX_PARTICIPANT_CODE` | Defaults to `claimguard.demo@hcx`; gateway is a stub either way. |

**Non-secret config** — set these in Render directly, not Doppler (they are
deploy-shaped, not secret; see `render.yaml`): `APP_CORS_ALLOWED_ORIGINS`,
`APP_EXPOSE_TECH=false`, `API_DOCS_ENABLED=false`, `CLAIM_SCOPE`,
`AUTO_APPROVE_MAX_SCORE`, `DECISION_SLA_HOURS`, `SENTRY_ENVIRONMENT`,
`EXTRACTION_WORKERS`.

> **Do not put `PORT` in Doppler.** Render injects it and Spring reads it
> (`server.port=${PORT:8080}`). A synced `PORT` will fight Render's.

> **Drop `GEMINI_API_KEY`.** It is in `backend/.env` but no code path reads it —
> `ExtractionConfig` only wires Cloudflare and Groq. Leftover from `STACK.md`.

### 2b. Console secrets (`claimguard-web` → `prd`)

| Secret | Notes |
|---|---|
| `AUTH_SECRET` | Generate fresh for production: `openssl rand -base64 32`. Do not reuse the dev one. |
| `AUTH_ZITADEL_ID` | Zitadel application client ID. |
| `AUTH_ZITADEL_SECRET` | Zitadel application client secret. |
| `AUTH_ZITADEL_ISSUER` | Same issuer as the backend's `OIDC_ISSUER_URI`. |
| `NEXT_PUBLIC_API_URL` | Public Render URL (§4). Ends up **in the browser bundle** — it is public by definition, but it must be set at *build* time. |
| `AUTH_URL` | `https://<your-vercel-domain>` — keeps the OAuth callback stable instead of inferring from `VERCEL_URL`. |
| `BACKEND_URL` | Optional. Only if the server should reach the API at a different address than the browser does. Otherwise omit and it falls back to `NEXT_PUBLIC_API_URL`. |
| `NEXT_PUBLIC_POSTHOG_KEY`, `NEXT_PUBLIC_POSTHOG_HOST` | Optional client analytics. |
| `NEXT_PUBLIC_TAWK_PROPERTY_ID`, `NEXT_PUBLIC_TAWK_WIDGET_ID` | Optional support chat. |

### 2c. Integrations

- **Doppler → Render**: Integrations → Render → authorize → map
  `claimguard-api/prd` to the `claimguard-api` service. Enable auto-redeploy on
  secret change if you want rotation to take effect immediately.
- **Doppler → Vercel**: Integrations → Vercel → map `claimguard-web/prd` to the
  **Production** environment, and `stg` to **Preview** if you want previews to
  work. This one matters more than on Render: `NEXT_PUBLIC_*` values are inlined
  at build time, so they must exist in Vercel *before* the build starts.

---

## 3. GHCR — build and push the image

The image is public-safe (it contains only the jar; every secret arrives as an
env var). Making the package public means Render needs no registry credentials.

```bash
cd /Users/parandhamareddybommaka/Desktop/sp

echo $GITHUB_PAT | docker login ghcr.io -u PARANDHAMAREDDYBOMMAKA --password-stdin

# --platform is NOT optional: you are on arm64, Render runs amd64 only.
docker buildx build \
  --platform linux/amd64 \
  -t ghcr.io/parandhamareddybommaka/cm-api:latest \
  -t ghcr.io/parandhamareddybommaka/cm-api:$(git rev-parse --short HEAD) \
  --push \
  backend
```

The PAT needs `write:packages`. Afterwards, in GitHub → Packages → `cm-api` →
Package settings, set visibility to **Public** (or keep it private and add the
credentials to Render in §4).

Sanity-check the image locally before shipping it:

```bash
docker run --rm -p 8080:8080 -e PORT=8080 \
  -e DATABASE_URL="postgres://..." \
  ghcr.io/parandhamareddybommaka/cm-api:latest
curl localhost:8080/api/ping
```

---

## 4. Render — `claimguard-api`

**New → Web Service → Existing Image.**

| Setting | Value |
|---|---|
| Image URL | `ghcr.io/parandhamareddybommaka/cm-api:latest` |
| Registry credential | None if the package is public; otherwise a GitHub PAT with `read:packages` |
| Name | `claimguard-api` |
| Region | Match your Neon region |
| Instance type | **Starter (512 MB) minimum — see the memory note below.** Not Free: free instances sleep after 15 min, which stalls `ExtractionSweeper` and makes cold starts ~60s. |
| Instances | **1. Do not scale this.** See below. |
| Health check path | `/api/ping` |
| Port | Leave blank — Render injects `PORT`, Spring reads it |

Environment variables to add in the dashboard (non-secret; secrets arrive via
Doppler):

```
APP_CORS_ALLOWED_ORIGINS = https://<your-vercel-domain>   # fill in after §5
APP_EXPOSE_TECH          = false
API_DOCS_ENABLED         = false
CLAIM_SCOPE              = org
AUTO_APPROVE_MAX_SCORE   = 24
DECISION_SLA_HOURS       = 24
SENTRY_ENVIRONMENT       = production
EXTRACTION_WORKERS       = 2
```

### Why one instance only

`ExtractionRecovery` runs `reclaimStalled(true)` at boot, which uses
`staleBefore = now` — it reclaims **every** unfinished extraction regardless of
whether another live instance still holds the lease. A second instance (or
Render's zero-downtime deploy overlap) will re-read documents the first one is
still working on: duplicate paid VLM calls. Not data-corrupting, but it costs
real money. Worth fixing before you ever scale — see §9.

### Memory

`FhirConfig.fhirContext()` builds `FhirContext.forR4()` eagerly at startup.
HAPI FHIR's R4 structure registry is a few hundred MB of heap on its own, on top
of Spring Boot 4, the AWS SDK and PDFBox. On a 512 MB Starter this is tight and
may OOM during a PDF rasterization. Two options:

- Bump to **Standard (2 GB)**, or
- Annotate that bean `@Lazy` — only `/fhir` and `/nhcx` touch it, so nothing
  else pays for it. One-line change; I can make it if you want.

---

## 5. Vercel — `claimguard-web`

**Add New → Project → import `PARANDHAMAREDDYBOMMAKA/cm`.**

| Setting | Value |
|---|---|
| Framework preset | Next.js |
| **Root Directory** | **`frontend`** ← the repo is a monorepo; this is the one setting that breaks the build if missed |
| Build command | default (`next build`) |
| Install command | default (`npm ci`) |
| Node version | 22 or 24 |

Environment variables come from the Doppler integration (§2c). If you want to
deploy before wiring Doppler, set at minimum `AUTH_SECRET`, the three
`AUTH_ZITADEL_*` values, and `NEXT_PUBLIC_API_URL` by hand first.

> `NEXT_PUBLIC_API_URL` is baked into the client bundle at build time. Changing
> it later needs a **redeploy**, not just an env-var edit.

---

## 6. Zitadel

In the application used by the console:

| Setting | Value |
|---|---|
| Redirect URI | `https://<your-vercel-domain>/api/auth/callback/zitadel` |
| Post-logout redirect URI | `https://<your-vercel-domain>` |
| **Token Settings → Auth Token Type** | **JWT** — not opaque. The backend validates the token as a JWT; with opaque tokens every API call 401s *after* a successful sign-in, which looks like a backend bug and is not. |
| Refresh token | Enabled — `lib/auth.ts` requests `offline_access` and refreshes on a 60s skew. |

Preview deployments get a different URL each time. Either add a Vercel preview
alias as a second redirect URI, or accept that sign-in only works on production.

---

## 7. Close the loop

Back in Render, set `APP_CORS_ALLOWED_ORIGINS` to the Vercel production domain
and redeploy.

CORS matters for exactly one call: `GET /api/ping` from
`components/dashboard/BackendStatus.tsx`, which the browser makes directly.
Everything else is proxied server-side through `backendFetch` or
`/api/backend/[...path]`, so a wrong value shows up as nothing more than a
permanently red "Backend offline" dot — not a broken console.

### Verify

```bash
curl https://claimguard-api.onrender.com/api/ping          # → ok, unauthenticated
curl https://claimguard-api.onrender.com/api/claims        # → 401, NOT 200
curl https://claimguard-api.onrender.com/api/stack         # → runtime versions
curl -I https://claimguard-api.onrender.com/docs           # → 404 (docs disabled)
```

`/api/claims` returning **200** means `OIDC_ISSUER_URI` did not reach the
container and the API is unauthenticated. Stop and fix that before anything
else.

Then in the console: sign in → create a claim → upload
`samples/clean-cataract-bill.pdf` → it should reach `EXTRACTED` and
auto-approve. Upload `samples/fake-inflated-total.pdf` → it should land in the
review queue with named reasons. Finally `/dashboard/audit` → **Verify** should
report the chain intact.

---

## 8. Handing deploys to CI

Once the manual path works:

1. Change `.github/workflows/image.yml` from `workflow_dispatch` to
   `push: branches: [main]` (add `needs: [backend]` gating on the existing test
   job so a red build never ships).
2. Render → Settings → **Deploy Hook**; put the URL in a repo secret
   `RENDER_DEPLOY_HOOK` and `curl -X POST "$RENDER_DEPLOY_HOOK"` as the last
   step of the image job.
3. Vercel deploys on push by itself once the Git integration is connected —
   nothing to add.

---

## 9. Known issues to fix before a real pilot

Found while reading the code; none of these block a demo deploy, all of them
matter with real claimants:

1. **Tenant scoping has holes.** `AccessPolicy.canSee()` is enforced in
   `ClaimService`, `ExtractionStore` and `ReviewService`, but **not** in
   `FhirClaimService` (`/fhir`, `/nhcx`), `ReviewController.decision()`, or any
   of `AuditController`. `/api/audit`, `/api/audit/export` and
   `/api/audit/claims/{id}` return every tenant's claim references, claimant
   names, filenames, invoice numbers and totals to any signed-in user.
2. **Null owner = visible to everyone.** `canSee` returns `true` when the
   claim's `owner_subject`/`owner_org` are null, so anything created before V6
   or before auth was switched on is readable by every user.
3. **Boot steals live leases** — the single-instance constraint in §4.
4. **The console renders without a session.** `app/dashboard/layout.tsx` only
   redirects when `session.error` is set; with no session at all it renders the
   shell and every panel shows a load error instead of sending you to sign-in.
5. **Docs drift.** `README.md` claims tenant scoping does not exist (V6 added
   it), says `middleware.ts` (it is `proxy.ts` since Next 16), and says
   migrations V1–V5 (V6 exists). `STACK.md` still names Clerk and Gemini; the
   code uses Zitadel, Cloudflare and Groq.
