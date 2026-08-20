import Link from "next/link";
import { FileText, PercentCircle, Flag, Wallet } from "lucide-react";
import BackendStatus from "@/components/dashboard/BackendStatus";
import StatCard from "@/components/dashboard/StatCard";
import LoadError from "@/components/dashboard/LoadError";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import { backendFetch, classifyFailure } from "@/lib/backend";
import { readItems } from "@/lib/page";
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

function Section({
  title,
  action,
  children,
}: {
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="mt-8">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-secondary">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-sm text-muted">{label}</dt>
      <dd className="text-sm font-medium tabular-nums text-ink">{value}</dd>
    </div>
  );
}

export default async function DashboardPage() {
  const [claimsResponse, metricsResponse] = await Promise.all([
    backendFetch("/api/claims?page=0&size=5"),
    backendFetch("/api/metrics"),
  ]);

  const recent = await readItems<ClaimSummary>(claimsResponse);
  const metrics: Metrics | null = metricsResponse.ok ? await metricsResponse.json() : null;
  const failure = metricsResponse.ok ? null : await classifyFailure(metricsResponse);
  const isEmpty = !failure && (metrics?.totalClaims ?? 0) === 0 && recent.length === 0;

  return (
    <div className="animate-in mx-auto max-w-6xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
          <p className="mt-1 text-sm text-muted">Claim intake, verification, and review at a glance.</p>
        </div>
        <div className="flex items-center gap-3">
          <BackendStatus />
          <ClaimFormDialog mode="create" />
        </div>
      </div>

      {failure ? <LoadError kind={failure} what="the dashboard metrics" /> : null}

      {isEmpty ? (
        <div className="mt-8 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-20 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <FileText className="size-6" />
          </span>
          <h2 className="mt-4 text-base font-semibold">No claims yet</h2>
          <p className="mt-1 max-w-sm text-sm text-muted">
            Create your first claim and upload a document. ClaimGuard extracts the details, scores the
            risk, and auto-approves it if nothing looks wrong.
          </p>
          <div className="mt-6">
            <ClaimFormDialog mode="create" />
          </div>
        </div>
      ) : null}

      {!failure && !isEmpty ? (
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

          <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
            <div>
              <h2 className="mb-3 text-sm font-semibold text-secondary">Throughput</h2>
              <div className="rounded-xl border border-border bg-surface p-5">
                <dl className="space-y-3">
                  <Row
                    label="Average decision time"
                    value={formatMinutes(metrics?.averageDecisionMinutes ?? null)}
                  />
                  <Row label="Decided" value={metrics?.decidedClaims ?? 0} />
                  <Row label="Auto-approved" value={metrics?.autoApproved ?? 0} />
                  <Row label="Approved by a reviewer" value={metrics?.reviewerApproved ?? 0} />
                  <Row label={`Open beyond ${metrics?.slaHours ?? 0}h SLA`} value={metrics?.openBeyondSla ?? 0} />
                  <Row label="Amount processed" value={formatCurrency(metrics?.amountProcessed ?? 0)} />
                </dl>
              </div>
            </div>

            <div>
              <h2 className="mb-3 text-sm font-semibold text-secondary">Risk</h2>
              <div className="space-y-4">
                <div className="rounded-xl border border-border bg-surface p-5">
                  <dl className="space-y-2">
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

                {metrics?.topSignals?.length ? (
                  <div className="rounded-xl border border-border bg-surface p-5">
                    <p className="text-xs font-medium uppercase tracking-wider text-subtle">Top signals</p>
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
              </div>
            </div>
          </div>

          <Section
            title="Audit chain"
            action={
              <Link href="/dashboard/audit" className="text-sm text-brand hover:underline">
                View audit trail
              </Link>
            }
          >
            <div className="rounded-xl border border-border bg-surface p-5">
              <dl className="space-y-3">
                <Row label="Entries" value={metrics?.auditEvents ?? 0} />
                <Row
                  label="Seal"
                  value={
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        metrics?.auditIntact ? "bg-success-soft text-success" : "bg-danger-soft text-danger"
                      }`}
                    >
                      {metrics?.auditIntact ? "Intact" : "Broken"}
                    </span>
                  }
                />
              </dl>
            </div>
          </Section>

          {recent.length > 0 ? (
            <Section
              title="Recent claims"
              action={
                <Link href="/dashboard/claims" className="text-sm text-brand hover:underline">
                  View all
                </Link>
              }
            >
              <div className="overflow-x-auto rounded-xl border border-border bg-surface">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left">
                      <th scope="col" className="px-5 py-2.5 text-xs font-medium uppercase tracking-wider text-subtle">
                        Reference
                      </th>
                      <th scope="col" className="px-5 py-2.5 text-xs font-medium uppercase tracking-wider text-subtle">
                        Status
                      </th>
                      <th scope="col" className="px-5 py-2.5 text-xs font-medium uppercase tracking-wider text-subtle">
                        Documents
                      </th>
                      <th
                        scope="col"
                        className="px-5 py-2.5 text-right text-xs font-medium uppercase tracking-wider text-subtle"
                      >
                        Created
                      </th>
                    </tr>
                  </thead>
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
                        <td className="px-5 py-3 tabular-nums text-secondary">{claim.documentCount}</td>
                        <td className="px-5 py-3 text-right text-secondary">
                          {new Date(claim.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
