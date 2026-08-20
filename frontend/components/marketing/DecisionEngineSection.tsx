import { GitBranch, ShieldOff, UserCheck } from "lucide-react";

const weights = [
  { severity: "Low", weight: "10" },
  { severity: "Medium", weight: "25" },
  { severity: "High", weight: "45" },
  { severity: "Critical", weight: "70" },
];

const bands = [
  { band: "Clean", range: "0", outcome: "Auto-approve" },
  { band: "Low", range: "1 – 24", outcome: "Auto-approve if no high or critical signal" },
  { band: "Medium", range: "25 – 49", outcome: "Human review" },
  { band: "High", range: "50 – 74", outcome: "Human review" },
  { band: "Critical", range: "75+", outcome: "Human review" },
];

export default function DecisionEngineSection() {
  return (
    <section id="decisioning" className="border-b border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Risk scoring & decision</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Every signal a claim trips is severity-weighted, summed and capped at 100, then banded
            into a decision.
          </p>
        </div>

        <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="overflow-hidden rounded-2xl border border-border bg-canvas">
            <div className="border-b border-border px-6 py-4">
              <h3 className="text-sm font-semibold text-ink">Signal severity weights</h3>
            </div>
            <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-muted">
                  <th className="px-6 py-2 font-medium">Severity</th>
                  <th className="px-6 py-2 font-medium">Weight added to score</th>
                </tr>
              </thead>
              <tbody>
                {weights.map(({ severity, weight }) => (
                  <tr key={severity} className="border-t border-border">
                    <td className="px-6 py-3 text-ink">{severity}</td>
                    <td className="px-6 py-3 font-mono text-secondary">{weight}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </div>

          <div className="overflow-hidden rounded-2xl border border-border bg-canvas">
            <div className="border-b border-border px-6 py-4">
              <h3 className="text-sm font-semibold text-ink">Risk bands & outcome</h3>
            </div>
            <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-muted">
                  <th className="px-6 py-2 font-medium">Band</th>
                  <th className="px-6 py-2 font-medium">Score</th>
                  <th className="px-6 py-2 font-medium">Routes to</th>
                </tr>
              </thead>
              <tbody>
                {bands.map(({ band, range, outcome }) => (
                  <tr key={band} className="border-t border-border">
                    <td className="px-6 py-3 text-ink">{band}</td>
                    <td className="px-6 py-3 font-mono text-secondary">{range}</td>
                    <td className="px-6 py-3 text-secondary">{outcome}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </div>
        </div>

        <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-3">
          <div className="rounded-2xl border border-border bg-canvas p-6">
            <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
              <ShieldOff className="size-5" />
            </div>
            <h3 className="mt-4 text-base font-semibold text-ink">Never auto-denies</h3>
            <p className="mt-2 text-sm leading-relaxed text-secondary">
              The system will auto-approve a clean claim, but it never auto-rejects one. Anything
              short of clean goes to a person.
            </p>
          </div>
          <div className="rounded-2xl border border-border bg-canvas p-6">
            <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
              <GitBranch className="size-5" />
            </div>
            <h3 className="mt-4 text-base font-semibold text-ink">Reasons in plain English</h3>
            <p className="mt-2 text-sm leading-relaxed text-secondary">
              Every decision, automated or human, is stored with the reasons behind it — not just
              the outcome.
            </p>
          </div>
          <div className="rounded-2xl border border-border bg-canvas p-6">
            <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
              <UserCheck className="size-5" />
            </div>
            <h3 className="mt-4 text-base font-semibold text-ink">Human review sticks</h3>
            <p className="mt-2 text-sm leading-relaxed text-secondary">
              A reviewer can approve, hold or escalate with a note. Once a human has decided, later
              automated re-assessment leaves that claim alone.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
