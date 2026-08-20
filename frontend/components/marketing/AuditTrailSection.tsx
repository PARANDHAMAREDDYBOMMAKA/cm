import { Download, FileSearch2, Link2, ShieldOff } from "lucide-react";

const points = [
  {
    icon: Link2,
    title: "One global hash chain",
    description:
      "Every action across every claim is appended to a single SHA-256 hash chain. Each entry seals the one before it.",
  },
  {
    icon: ShieldOff,
    title: "Append-only in the database",
    description:
      "A PostgreSQL trigger rejects UPDATE and DELETE on the audit table outright — this isn't just application-level discipline.",
  },
  {
    icon: FileSearch2,
    title: "Independently verifiable",
    description: "A verify endpoint recomputes the entire chain from scratch and names the first broken link, if there is one.",
  },
  {
    icon: Download,
    title: "Exportable, and the export is audited",
    description: "The full trail can be exported as CSV. Running that export is itself written into the chain.",
  },
];

export default function AuditTrailSection() {
  return (
    <section id="audit" className="border-b border-border bg-canvas py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Tamper-evident audit trail</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Every step a claim goes through — upload, extraction, every fraud check, every
            decision — is written to a chain that cannot be quietly edited later.
          </p>
        </div>

        <div className="mt-12 grid grid-cols-1 gap-5 sm:grid-cols-2">
          {points.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-2xl border border-border bg-surface p-6">
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
