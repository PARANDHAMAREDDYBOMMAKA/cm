import { FileText, Flag, ShieldCheck, ShieldX, UploadCloud } from "lucide-react";
import BackendStatus from "@/components/dashboard/BackendStatus";
import StatCard from "@/components/dashboard/StatCard";

export default function DashboardPage() {
  return (
    <div className="mx-auto max-w-6xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-muted">Claim intake, verification, and review at a glance.</p>
        </div>
        <BackendStatus />
      </div>

      <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Claims processed" value="0" hint="All time" icon={FileText} tone="brand" />
        <StatCard label="Auto-approved" value="0" hint="Clean and verified" icon={ShieldCheck} tone="success" />
        <StatCard label="Flagged for review" value="0" hint="Needs a human" icon={Flag} tone="warning" />
        <StatCard label="Fraud caught" value="0" hint="Blocked before payout" icon={ShieldX} tone="danger" />
      </div>

      <div className="mt-6 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-16 text-center">
        <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
          <UploadCloud className="size-6" />
        </span>
        <h2 className="mt-4 text-base font-semibold">No claims yet</h2>
        <p className="mt-1 max-w-sm text-sm text-muted">
          Upload a claim document to run extraction, authenticity checks, and a fraud score.
        </p>
        <button
          type="button"
          disabled
          className="mt-5 inline-flex cursor-not-allowed items-center gap-2 rounded-lg bg-canvas px-4 py-2 text-sm font-medium text-muted"
        >
          <UploadCloud className="size-4" />
          Upload a claim
        </button>
      </div>
    </div>
  );
}
