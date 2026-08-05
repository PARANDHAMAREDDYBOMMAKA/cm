import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { backendFetch, classifyFailure } from "@/lib/backend";
import { readItems } from "@/lib/page";
import LoadError from "@/components/dashboard/LoadError";
import { BAND_STYLES, isFlagged, type RiskBand } from "@/lib/risk";
import { outcomeLabel, OUTCOME_STYLES, type DecisionOutcome } from "@/lib/decision";
import { formatRelativeAge } from "@/lib/format";

export const dynamic = "force-dynamic";

type ClaimSummary = {
  id: string;
  reference: string;
  claimantName: string | null;
  status: string;
  documentCount: number;
  createdAt: string;
  riskScore: number | null;
  riskBand: RiskBand | null;
  decisionOutcome: DecisionOutcome | null;
};

export default async function ReviewQueuePage() {
  const response = await backendFetch("/api/claims/queue/review?page=0&size=200");
  const claims = await readItems<ClaimSummary>(response);
  const failure = response.ok ? null : await classifyFailure(response);

  return (
    <div className="animate-in mx-auto max-w-6xl">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Review queue</h1>
          <p className="mt-1 text-sm text-muted">Claims sorted by risk, highest first.</p>
        </div>
      </div>

      {failure ? (
        <LoadError kind={failure} what="the review queue" />
      ) : claims.length === 0 ? (
        <div className="mt-6 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-16 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <ShieldAlert className="size-6" />
          </span>
          <h2 className="mt-4 text-base font-semibold">Nothing waiting on a human</h2>
          <p className="mt-1 max-w-sm text-sm text-muted">
            Claims land here once they are flagged for review or need a decision.
          </p>
        </div>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-border bg-surface">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-subtle">
                <th className="px-5 py-3 font-medium">Reference</th>
                <th className="px-5 py-3 font-medium">Claimant</th>
                <th className="px-5 py-3 font-medium">Risk</th>
                <th className="px-5 py-3 font-medium">Decision</th>
                <th className="px-5 py-3 font-medium">Documents</th>
                <th className="px-5 py-3 font-medium">Age</th>
              </tr>
            </thead>
            <tbody>
              {claims.map((claim) => {
                const flagged = claim.riskBand ? isFlagged(claim.riskBand) : false;
                return (
                  <tr
                    key={claim.id}
                    className={`border-b border-border last:border-0 hover:bg-canvas ${
                      flagged ? "border-l-2 border-l-danger" : ""
                    }`}
                  >
                    <td className="px-5 py-3">
                      <Link
                        href={`/dashboard/claims/${claim.id}`}
                        className="font-medium text-ink transition-colors hover:text-brand"
                      >
                        {claim.reference}
                      </Link>
                    </td>
                    <td className="px-5 py-3 text-secondary">{claim.claimantName ?? "—"}</td>
                    <td className="px-5 py-3">
                      {claim.riskBand ? (
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-medium ${BAND_STYLES[claim.riskBand]}`}
                        >
                          {claim.riskBand} {claim.riskScore}
                        </span>
                      ) : (
                        <span className="text-xs text-subtle">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      {claim.decisionOutcome ? (
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-medium ${OUTCOME_STYLES[claim.decisionOutcome]}`}
                        >
                          {outcomeLabel(claim.decisionOutcome)}
                        </span>
                      ) : (
                        <span className="text-xs text-subtle">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3 tabular-nums text-secondary">{claim.documentCount}</td>
                    <td className="px-5 py-3 text-secondary">{formatRelativeAge(claim.createdAt)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
