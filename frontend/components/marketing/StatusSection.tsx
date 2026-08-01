import { ImageOff, Send, Users } from "lucide-react";

const items = [
  {
    icon: ImageOff,
    title: "AI-generated-image detection",
    description:
      "The forensics pipeline has a slot for it, but no free provider is wired in yet, so this signal is skipped on every claim today.",
  },
  {
    icon: Send,
    title: "NHCX submission",
    description:
      "FHIR R4 Bundles are generated correctly and are ready to send. Actually submitting them to a live exchange isn't wired up — no endpoint is configured.",
  },
  {
    icon: Users,
    title: "Single-tenant",
    description:
      "There is no organisation-level data isolation yet. Every signed-in user currently sees every claim in the system.",
  },
];

export default function StatusSection() {
  return (
    <section id="status" className="bg-canvas py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Where this is today</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            v1 is deliberately scoped. Here is exactly what isn&apos;t live yet, stated plainly
            rather than left for you to discover.
          </p>
        </div>

        <div className="mt-12 grid grid-cols-1 gap-5 sm:grid-cols-3">
          {items.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-2xl border border-border bg-surface p-6">
              <div className="flex items-center justify-between">
                <div className="flex size-10 items-center justify-center rounded-lg bg-warning-soft text-warning">
                  <Icon className="size-5" />
                </div>
                <span className="rounded-full bg-warning-soft px-2.5 py-1 text-[11px] font-medium text-warning">
                  Not yet live
                </span>
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
