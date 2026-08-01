import { ArrowRight } from "lucide-react";

const jumps = [
  { href: "#extraction", label: "Extraction" },
  { href: "#duplicates", label: "Duplicates" },
  { href: "#forensics", label: "Forensics" },
  { href: "#consistency", label: "Consistency" },
  { href: "#decisioning", label: "Decisioning" },
  { href: "#audit", label: "Audit trail" },
  { href: "#interop", label: "Interop & console" },
  { href: "#security", label: "Security" },
  { href: "#status", label: "Where this is today" },
];

export default function FeaturesHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-canvas">
      <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[26rem] bg-gradient-to-b from-brand-soft to-transparent" />

      <div className="mx-auto max-w-6xl px-6 pb-16 pt-20 sm:pb-20 sm:pt-28">
        <div className="animate-in mx-auto max-w-3xl text-center">
          <span className="inline-flex items-center rounded-full border border-border bg-surface px-3 py-1 text-xs font-medium text-secondary">
            Full capability breakdown
          </span>

          <h1 className="mt-6 text-4xl font-semibold leading-tight tracking-tight text-ink sm:text-5xl">
            Every signal ClaimGuard checks before a claim is paid
          </h1>

          <p className="mt-5 text-base leading-relaxed text-secondary sm:text-lg">
            The landing page gives you the summary. This is the detailed walkthrough — what gets
            extracted, what gets flagged, how the risk score is built, and exactly what is and
            isn&apos;t wired up yet in v1.
          </p>
        </div>

        <nav
          aria-label="Jump to section"
          className="mx-auto mt-10 flex max-w-4xl flex-wrap items-center justify-center gap-2"
        >
          {jumps.map((jump) => (
            <a
              key={jump.href}
              href={jump.href}
              className="group flex items-center gap-1 rounded-full border border-border bg-surface px-3.5 py-1.5 text-xs font-medium text-secondary transition-colors hover:border-border-strong hover:text-ink"
            >
              {jump.label}
              <ArrowRight className="size-3 -rotate-45 opacity-0 transition-opacity group-hover:opacity-100" />
            </a>
          ))}
        </nav>
      </div>
    </section>
  );
}
