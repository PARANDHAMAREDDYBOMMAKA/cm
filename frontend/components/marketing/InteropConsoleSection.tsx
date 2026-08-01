import { BarChart3, FileJson, ListTree, Settings2, Send } from "lucide-react";

const bundleResources = ["Patient", "Organization", "Coverage", "Claim"];

const consoleItems = [
  {
    icon: BarChart3,
    title: "Dashboard",
    description:
      "Straight-through-processing rate, leakage caught, average decision time, SLA breach count, risk-band distribution and top fraud signals.",
  },
  {
    icon: ListTree,
    title: "Flagged-claims queue",
    description: "A review queue of flagged claims sorted by risk, for whoever is triaging that day.",
  },
  {
    icon: FileJson,
    title: "Audit views",
    description: "Per-claim and global views over the same hash-chained audit trail.",
  },
  {
    icon: Settings2,
    title: "Settings",
    description: "A settings page that shows exactly which capabilities are configured in this deployment.",
  },
];

export default function InteropConsoleSection() {
  return (
    <section id="interop" className="border-b border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Interop & console</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Claim data doesn&apos;t stay locked inside ClaimGuard, and the pipeline is visible from
            a console, not just an API.
          </p>
        </div>

        <div className="mt-12 rounded-2xl border border-border bg-canvas p-6">
          <div className="flex items-start gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-brand">
              <FileJson className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-ink">NHCX-ready FHIR R4 output</h3>
              <p className="mt-1 text-sm leading-relaxed text-secondary">
                Every claim can be produced as a FHIR R4 Bundle via HAPI FHIR, with identifiers,
                diagnosis, billable period and itemised amounts in INR.
              </p>
            </div>
          </div>
          <ul className="mt-5 flex flex-wrap gap-2">
            {bundleResources.map((resource) => (
              <li
                key={resource}
                className="rounded-full border border-border bg-surface px-3 py-1.5 font-mono text-xs font-medium text-secondary"
              >
                {resource}
              </li>
            ))}
          </ul>
          <div className="mt-5 flex items-start gap-3 rounded-xl border border-warning/20 bg-warning-soft p-4">
            <Send className="mt-0.5 size-4 shrink-0 text-warning" />
            <p className="text-sm leading-relaxed text-secondary">
              <span className="font-medium text-ink">Not yet submitted anywhere.</span> The Bundle is
              generated and ready, but sending it to a live NHCX exchange is not wired up — no
              exchange endpoint is configured in this build.
            </p>
          </div>
        </div>

        <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2">
          {consoleItems.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-2xl border border-border bg-canvas p-6">
              <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
                <Icon className="size-5" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-ink">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-secondary">{description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
