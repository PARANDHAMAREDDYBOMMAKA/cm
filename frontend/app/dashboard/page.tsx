import Link from "next/link";
import { FileText, Flag, ShieldCheck, ShieldX } from "lucide-react";
import BackendStatus from "@/components/dashboard/BackendStatus";
import StatCard from "@/components/dashboard/StatCard";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import { backendFetch } from "@/lib/backend";

export const dynamic = "force-dynamic";

type ClaimSummary = {
  id: string;
  reference: string;
  status: string;
  documentCount: number;
  createdAt: string;
};

const statusStyles: Record<string, string> = {
  RECEIVED: "bg-brand-soft text-brand",
  PROCESSING: "bg-warning-soft text-warning",
  APPROVED: "bg-success-soft text-success",
  FLAGGED: "bg-danger-soft text-danger",
};

export default async function DashboardPage() {
  const response = await backendFetch("/api/claims");
  const claims: ClaimSummary[] = response.ok ? await response.json() : [];
  const total = claims.length;
  const approved = claims.filter((claim) => claim.status === "APPROVED").length;
  const flagged = claims.filter((claim) => claim.status === "FLAGGED").length;
  const recent = claims.slice(0, 5);

  return (
    <div className="animate-in mx-auto max-w-6xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-muted">Claim intake, verification, and review at a glance.</p>
        </div>
        <div className="flex items-center gap-3">
          <BackendStatus />
          <ClaimFormDialog mode="create" />
        </div>
      </div>

      <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Claims processed" value={String(total)} hint="All time" icon={FileText} tone="brand" />
        <StatCard label="Auto-approved" value={String(approved)} hint="Clean and verified" icon={ShieldCheck} tone="success" />
        <StatCard label="Flagged for review" value={String(flagged)} hint="Needs a human" icon={Flag} tone="warning" />
        <StatCard label="Fraud caught" value="0" hint="Blocked before payout" icon={ShieldX} tone="danger" />
      </div>

      {recent.length === 0 ? (
        <div className="mt-6 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-16 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <FileText className="size-6" />
          </span>
          <h2 className="mt-4 text-base font-semibold">No claims yet</h2>
          <p className="mt-1 max-w-sm text-sm text-muted">Create your first claim to start uploading documents.</p>
        </div>
      ) : (
        <div className="mt-8">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-secondary">Recent claims</h2>
            <Link href="/dashboard/claims" className="text-sm text-brand hover:underline">
              View all
            </Link>
          </div>
          <div className="overflow-x-auto rounded-xl border border-border bg-surface">
            <table className="w-full text-sm">
              <tbody>
                {recent.map((claim) => (
                  <tr key={claim.id} className="border-b border-border last:border-0 hover:bg-canvas">
                    <td className="px-5 py-3">
                      <Link
                        href={`/dashboard/claims/${claim.id}`}
                        className="font-medium text-ink transition-colors hover:text-brand"
                      >
                        {claim.reference}
                      </Link>
                    </td>
                    <td className="px-5 py-3">
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusStyles[claim.status] ?? "bg-canvas text-muted"}`}
                      >
                        {claim.status}
                      </span>
                    </td>
                    <td className="px-5 py-3 tabular-nums text-secondary">{claim.documentCount} docs</td>
                    <td className="px-5 py-3 text-right text-secondary">
                      {new Date(claim.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
