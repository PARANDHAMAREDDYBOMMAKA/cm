import Link from "next/link";
import { FileText, PercentCircle, Flag, Wallet } from "lucide-react";
import BackendStatus from "@/components/dashboard/BackendStatus";
import StatCard from "@/components/dashboard/StatCard";
import LoadError from "@/components/dashboard/LoadError";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import { backendFetch, classifyFailure } from "@/lib/backend";
import { statusStyle } from "@/lib/claim";
import { BAND_STYLES, signalLabel, type RiskBand } from "@/lib/risk";
import { formatCurrency, formatMinutes } from "@/lib/format";

export const dynamic = "force-dynamic";

type ClaimSummary = {
  id: string;
  reference: string;
  status: string;
  documentCount: number;
  createdAt: string;
};

type Metrics = {
  totalClaims: number;
  decidedClaims: number;
  autoApproved: number;
  needsReview: number;
  reviewerApproved: number;
  flagged: number;
  escalated: number;
  awaitingReview: number;
  straightThroughRate: number;
  amountProcessed: string;
  leakageCaught: string;
  averageDecisionMinutes: number | null;
  slaHours: number;
  openBeyondSla: number;
  riskBands: Partial<Record<RiskBand, number>>;
  topSignals: { type: string; count: number }[];
  auditEvents: number;
  auditIntact: boolean;
};

export default async function DashboardPage() {
  const [claimsResponse, metricsResponse] = await Promise.all([
    backendFetch("/api/claims"),
    backendFetch("/api/metrics"),
  ]);

  const claims: ClaimSummary[] = claimsResponse.ok ? await claimsResponse.json() : [];
  const metrics: Metrics | null = metricsResponse.ok ? await metricsResponse.json() : null;
  const failure = metricsResponse.ok ? null : await classifyFailure(metricsResponse);
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

      {failure ? (
        <LoadError kind={failure} what="the dashboard metrics" />
      ) : (
        <>
          <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              label="Claims processed"
              value={String(metrics?.totalClaims ?? 0)}
              hint="All time"
              icon={FileText}
              tone="brand"
            />
            <StatCard
              label="Straight-through rate"
              value={`${metrics?.straightThroughRate ?? 0}%`}
              hint="Auto-approved without a human"
              icon={PercentCircle}
              tone="success"
            />
            <StatCard
              label="Leakage caught"
              value={formatCurrency(metrics?.leakageCaught ?? 0)}
              hint="Blocked before payout"
              icon={Wallet}
              tone="danger"
            />
            <StatCard
              label="Awaiting review"
              value={String(metrics?.awaitingReview ?? 0)}
              hint="Sitting in the review queue"
              icon={Flag}
              tone="warning"
            />
          </div>

          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
            <div className="rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-secondary">Turnaround</h2>
              <dl className="mt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <dt className="text-sm text-muted">Average decision time</dt>
                  <dd className="text-sm font-medium tabular-nums text-ink">
                    {formatMinutes(metrics?.averageDecisionMinutes ?? null)}
                  </dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="text-sm text-muted">Open beyond {metrics?.slaHours ?? 0}h SLA</dt>
                  <dd className="text-sm font-medium tabular-nums text-ink">{metrics?.openBeyondSla ?? 0}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="text-sm text-muted">Amount processed</dt>
                  <dd className="text-sm font-medium tabular-nums text-ink">
                    {formatCurrency(metrics?.amountProcessed ?? 0)}
                  </dd>
                </div>
              </dl>
            </div>

            <div className="rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-secondary">Audit chain</h2>
              <dl className="mt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <dt className="text-sm text-muted">Entries</dt>
                  <dd className="text-sm font-medium tabular-nums text-ink">{metrics?.auditEvents ?? 0}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="text-sm text-muted">Seal</dt>
                  <dd
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      metrics?.auditIntact ? "bg-success-soft text-success" : "bg-danger-soft text-danger"
                    }`}
                  >
                    {metrics?.auditIntact ? "Intact" : "Broken"}
                  </dd>
                </div>
              </dl>
              <Link href="/dashboard/audit" className="mt-4 inline-block text-sm text-brand hover:underline">
                View audit trail
              </Link>
            </div>

            <div className="rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-secondary">Risk bands</h2>
              <dl className="mt-4 space-y-2">
                {Object.entries(metrics?.riskBands ?? {}).length === 0 ? (
                  <p className="text-sm text-subtle">No claims assessed yet.</p>
                ) : (
                  Object.entries(metrics?.riskBands ?? {}).map(([band, count]) => (
                    <div key={band} className="flex items-center justify-between">
                      <dt>
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-medium ${BAND_STYLES[band as RiskBand]}`}
                        >
                          {band}
                        </span>
                      </dt>
                      <dd className="text-sm font-medium tabular-nums text-ink">{count}</dd>
                    </div>
                  ))
                )}
              </dl>
            </div>
          </div>

          {metrics?.topSignals?.length ? (
            <div className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-secondary">Top fraud signals</h2>
              <ul className="mt-3 space-y-2">
                {metrics.topSignals.map((signal) => (
                  <li key={signal.type} className="flex items-center justify-between text-sm">
                    <span className="text-secondary">{signalLabel(signal.type)}</span>
                    <span className="font-medium tabular-nums text-ink">{signal.count}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </>
      )}

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
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusStyle(claim.status)}`}>
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
