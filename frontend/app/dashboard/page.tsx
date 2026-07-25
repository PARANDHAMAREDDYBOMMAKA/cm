import BackendStatus from "@/components/dashboard/BackendStatus";
import StatCard from "@/components/dashboard/StatCard";

export default function DashboardPage() {
  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight">Dashboard</h2>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Overview of claim intake, verification, and review.
          </p>
        </div>
        <BackendStatus />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Claims processed" value="0" hint="All time" />
        <StatCard label="Auto-approved" value="0" hint="Clean and verified" accent="emerald" />
        <StatCard label="Flagged for review" value="0" hint="Needs a human" accent="amber" />
        <StatCard label="Fraud caught" value="0" hint="Blocked before payout" accent="red" />
      </div>

      <div className="mt-8 rounded-xl border border-dashed border-zinc-300 bg-white p-12 text-center dark:border-zinc-700 dark:bg-zinc-900">
        <h3 className="text-base font-medium">No claims yet</h3>
        <p className="mx-auto mt-1 max-w-sm text-sm text-zinc-500 dark:text-zinc-400">
          Upload a claim document to run extraction, authenticity checks, and a fraud score.
        </p>
        <button
          type="button"
          disabled
          className="mt-5 cursor-not-allowed rounded-lg bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-400 dark:bg-zinc-800 dark:text-zinc-600"
        >
          Upload a claim
        </button>
      </div>
    </div>
  );
}
