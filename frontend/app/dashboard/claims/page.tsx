import Link from "next/link";
import { FileText } from "lucide-react";
import { backendFetch, classifyFailure } from "@/lib/backend";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import LoadError from "@/components/dashboard/LoadError";
import { BAND_STYLES, type RiskBand } from "@/lib/risk";

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
};

const statusStyles: Record<string, string> = {
  RECEIVED: "bg-brand-soft text-brand",
  PROCESSING: "bg-warning-soft text-warning",
  EXTRACTED: "bg-brand-soft text-brand",
  FAILED: "bg-danger-soft text-danger",
  APPROVED: "bg-success-soft text-success",
  FLAGGED: "bg-danger-soft text-danger",
};

export default async function ClaimsPage() {
  const response = await backendFetch("/api/claims");
  const claims: ClaimSummary[] = response.ok ? await response.json() : [];
  const failure = response.ok ? null : await classifyFailure(response);

  return (
    <div className="animate-in mx-auto max-w-6xl">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Claims</h1>
          <p className="mt-1 text-sm text-muted">Every claim received, with its documents and status.</p>
        </div>
        <ClaimFormDialog mode="create" />
      </div>

      {failure ? (
        <LoadError kind={failure} what="claims" />
      ) : claims.length === 0 ? (
        <div className="mt-6 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-16 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <FileText className="size-6" />
          </span>
          <h2 className="mt-4 text-base font-semibold">No claims yet</h2>
          <p className="mt-1 max-w-sm text-sm text-muted">Create your first claim to start uploading documents.</p>
        </div>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-border bg-surface">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-subtle">
                <th className="px-5 py-3 font-medium">Reference</th>
                <th className="px-5 py-3 font-medium">Claimant</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium">Risk</th>
                <th className="px-5 py-3 font-medium">Documents</th>
                <th className="px-5 py-3 font-medium">Created</th>
              </tr>
            </thead>
            <tbody>
              {claims.map((claim) => (
                <tr key={claim.id} className="border-b border-border last:border-0 hover:bg-canvas">
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
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusStyles[claim.status] ?? "bg-canvas text-muted"}`}
                    >
                      {claim.status}
                    </span>
                  </td>
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
                  <td className="px-5 py-3 tabular-nums text-secondary">{claim.documentCount}</td>
                  <td className="px-5 py-3 text-secondary">{new Date(claim.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
