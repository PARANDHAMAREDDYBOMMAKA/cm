import { GitBranch, ScanEye, ShieldCheck, UploadCloud } from "lucide-react";

const steps = [
  {
    icon: UploadCloud,
    title: "Upload",
    description: "Drop in the claim documents — bills, discharge summaries, ID proofs, prescriptions.",
  },
  {
    icon: ScanEye,
    title: "Read",
    description:
      "A vision model extracts patient, provider, diagnosis, line items and amounts, each with a per-field confidence score.",
  },
  {
    icon: ShieldCheck,
    title: "Verify",
    description:
      "Duplicate, forensic, device and consistency checks run automatically and produce a 0–100 risk score.",
  },
  {
    icon: GitBranch,
    title: "Decide",
    description:
      "Clean claims auto-approve, suspicious ones route to review — every step sealed into the audit chain.",
  },
];

export default function HowItWorks() {
  return (
    <section id="how" className="border-b border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">
            One pipeline, from upload to decision
          </h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Every claim moves through the same four stages, and every stage leaves a record.
          </p>
        </div>

        <div className="relative mt-16 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <div className="absolute inset-x-0 top-9 hidden h-px bg-border lg:block" aria-hidden="true" />
          {steps.map(({ icon: Icon, title, description }, index) => (
            <div key={title} className="relative rounded-2xl border border-border bg-canvas p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand text-white">
                  <Icon className="size-4.5" />
                </div>
                <span className="font-mono text-xs text-subtle">Step {index + 1}</span>
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
