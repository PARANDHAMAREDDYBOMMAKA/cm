# ClaimGuard — "caught the fake" demo script

A scripted walkthrough for showing ClaimGuard to a hospital or TPA prospect: one clean claim that
sails through, one faked claim that gets caught, and the audit trail that proves nothing was
altered afterwards. Total run time: 6–8 minutes.

## Before you're in front of anyone

**One external prerequisite:** in the Zitadel console, open the application used for
`AUTH_ZITADEL_ID` and check **Token Settings → Auth Token Type = JWT** (not "Opaque"). ClaimGuard's
backend validates the access token as a signed JWT (`spring.security.oauth2.resourceserver.jwt`);
an opaque token will pass sign-in but every API call will come back `401`.

Everything else is the normal local setup:

1. Backend — from `backend/`:
   ```bash
   ./mvnw spring-boot:run
   ```
   Confirms it's up: `curl http://localhost:8080/api/ping` → `{"status":"ok",...}`.
2. Sample documents exist in `samples/` (this directory). If not, regenerate them — see
   `samples/README.md`.
3. Frontend — from `frontend/`:
   ```bash
   npm run dev
   ```
   Open `http://localhost:3000`.

Have `samples/clean-cataract-bill.pdf` and `samples/fake-exact-duplicate.pdf` ready to pick from a
file dialog.

## Beat 1 — Sign in

Open `http://localhost:3000` and sign in through Zitadel. Land on `/dashboard`.

**Say:** "This is the reviewer console. Right now nothing's been processed, so the numbers are all
zero — straight-through rate, claims processed, amount leaked. We're going to change that live."

## Beat 2 — The clean claim

1. Go to **Claims → New Claim**.
2. In the dialog, set **Reference** to `CLM-DEMO-1` (the hint says "blank = auto-generate", but
   naming it makes the story easier to follow), leave claimant name/note as you like, save.
3. Open the new claim and upload `clean-cataract-bill.pdf`.

**Say:** "That PDF just went to our document reader — a vision-language model — which pulled out
the patient, the dates, the diagnosis, and every line item. At the same time we're fingerprinting
the file itself: hash, perceptual hash, PDF metadata."

**Expected on screen:** the document row shows extraction status moving to *completed* within a
few seconds (poll or refresh). The **risk panel** shows *"No fraud signals of concern"*, score
`0/100`. The **decision panel** shows **Auto-approved**, with the reasons *"No fraud or
consistency signals of concern"* and *"Every document was read and the figures line up."* The
claim's status badge flips to `APPROVED`.

4. Go back to `/dashboard`.

**Expected on screen:** **Straight-through rate** and **claims processed** have both moved off
zero — this claim resolved itself with no human involved.

## Beat 3 — The faked claim

1. **Claims → New Claim** again, reference `CLM-DEMO-2`.
2. Upload `fake-exact-duplicate.pdf` — this file is a byte-for-byte copy of
   `clean-cataract-bill.pdf` you just uploaded on `CLM-DEMO-1`, saved under a different name to
   imitate someone resubmitting the same bill on a second claim.

**Say:** "Same trick fraud rings actually run — take a paid bill and resubmit it, sometimes with a
new patient name typed over the top, sometimes not even bothering to change the file. Watch what
happens."

**Expected on screen:** once extraction completes, the **risk panel** flips to *"Flagged for
review"* with a score of at least `70/100`. The headline signal reads: *"This exact file was
already submitted on claim CLM-DEMO-1"* — naming the earlier claim by reference, with severity
**CRITICAL** and weight `+70`. Because the copy also carries the same invoice number, a second
signal, **reused invoice number** (`+45`), normally fires alongside it, which pushes the score to
the cap and the band to **CRITICAL**. The **decision panel** shows **Needs review** with the
duplicate message as its top reason, and the claim's status badge is `FLAGGED`.

> On the band: severity weights are summed and capped at 100, and `CRITICAL` starts at 75. A lone
> critical signal scores 70, which reads as **HIGH** — still flagged, still routed to a human. Say
> "high risk" rather than naming a band if you have not rehearsed against your own data.

**Say:** "It's not a heuristic guess — that's a SHA-256 match against a document we already have
on file, and it's telling the reviewer exactly which earlier claim to go compare against."

> If your Cloudflare embeddings key is set, a third signal — **matching contents** — may also
> appear, since the two documents are semantically identical. That is expected, not a double count.

## Beat 4 — The reviewer's turn

1. Go to **Review queue** (or open `CLM-DEMO-2` directly) and look at the **decision reasons**
   list and the fraud signal detail (severity, weight, and the `otherClaim`/`otherDocument`
   details attached to the signal).
2. Point at the three review actions: **Approve**, **Hold**, **Escalate**.

**Say:** "A reviewer isn't stuck with the machine's verdict — they can approve it anyway if there's
a good explanation, hold it for more documents, or escalate straight to investigations. Whatever
they pick is recorded against their name, not the system's."

3. Click **Escalate** (or **Hold**, if you'd rather show it can come back later) and show the
   claim status and decision outcome update immediately.

## Beat 5 — The audit trail

Go to **Audit**.

**Say:** "Every one of those actions — claim created, document uploaded, risk assessed, decision
recorded, reviewer action — is one row here, in order, and each row's hash is computed from its
own contents plus the previous row's hash. Change or delete anything upstream and every hash after
it stops matching."

**Expected on screen:** the event list shows, in order, the `CLAIM_CREATED`, document upload,
`RISK_ASSESSED`, `DECISION_RECORDED`, and reviewer-action entries for both `CLM-DEMO-1` and
`CLM-DEMO-2`. The seal badge at the top reads **intact** (backed by `GET /api/audit/verify`,
which recomputes every hash in the chain server-side).

**Say:** "That's not a log you edit after the fact to make a report look better — it's a hash
chain a court or an auditor can independently re-verify. If someone deletes a row from the table
directly, this badge is what catches it."

## Wrap-up line

"So: a clean claim went from upload to auto-approved with no one touching it, and a duplicate
submission got caught, explained in plain English, and routed to a human — all inside the same
straight-through pipeline, with a tamper-evident record of every step in between."

## If something doesn't line up

- **Extraction never completes / stays "pending":** no document reader is configured — check
  `CLOUDFLARE_API_TOKEN`/`CLOUDFLARE_ACCOUNT_ID` or `GROQ_API_KEY` are set on the backend.
- **API calls 401 right after sign-in:** almost always the Zitadel Token Settings prerequisite
  above — Auth Token Type must be JWT.
- **CLM-DEMO-2 doesn't flag:** confirm you actually uploaded `fake-exact-duplicate.pdf` (not
  `clean-cataract-bill.pdf` again) and that `CLM-DEMO-1`'s upload had already finished extracting
  — the duplicate check compares against documents already fingerprinted on other claims.
