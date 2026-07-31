import Link from "next/link";
import { ArrowRight, FileCheck2, Hash, ScanText } from "lucide-react";

const stats = [
  {
    icon: ScanText,
    value: "16/16",
    label: "fields extracted from a real synthetic hospital bill",
  },
  {
    icon: FileCheck2,
    value: "9",
    label: "fraud signal types checked on every claim",
  },
  {
    icon: Hash,
    value: "100%",
    label: "of actions hash-chained into the audit trail",
  },
];

export default function Hero({
  ctaHref,
  ctaLabel,
}: {
  ctaHref: string;
  ctaLabel: string;
}) {
  return (
    <section className="relative overflow-hidden border-b border-border bg-canvas">
      <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[32rem] bg-gradient-to-b from-brand-soft to-transparent" />

      <div className="mx-auto max-w-6xl px-6 pb-20 pt-20 sm:pb-28 sm:pt-28">
        <div className="animate-in mx-auto max-w-3xl text-center">
          <span className="inline-flex items-center rounded-full border border-border bg-surface px-3 py-1 text-xs font-medium text-secondary">
            Claims intelligence for hospitals and TPAs
          </span>

          <h1 className="mt-6 text-4xl font-semibold leading-tight tracking-tight text-ink sm:text-5xl">
            Verify every claim before it is paid.
          </h1>

          <p className="mt-5 text-base leading-relaxed text-secondary sm:text-lg">
            ClaimGuard reads claim documents, catches duplicates and tampering, auto-approves the
            clean ones, and routes the rest to a human — with a tamper-evident audit trail behind
            every decision.
          </p>

          <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link
              href={ctaHref}
              className="group flex w-full items-center justify-center gap-2 rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-hover sm:w-auto"
            >
              {ctaLabel}
              <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
            </Link>
            <a
              href="#how"
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-border-strong bg-surface px-5 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-canvas sm:w-auto"
            >
              See how it works
            </a>
          </div>
        </div>

        <div className="mx-auto mt-16 grid max-w-4xl grid-cols-1 gap-4 sm:grid-cols-3">
          {stats.map(({ icon: Icon, value, label }) => (
            <div
              key={label}
              className="rounded-2xl border border-border bg-surface p-5 text-center shadow-sm"
            >
              <div className="mx-auto flex size-9 items-center justify-center rounded-lg bg-brand-soft text-brand">
                <Icon className="size-4.5" />
              </div>
              <p className="mt-3 text-2xl font-semibold tracking-tight text-ink">{value}</p>
              <p className="mt-1 text-xs leading-relaxed text-muted">{label}</p>
            </div>
          ))}
        </div>
        <p className="mt-4 text-center text-xs text-subtle">
          v1 demo, measured against a synthetic claims set — not production volume figures.
        </p>
      </div>
    </section>
  );
}
