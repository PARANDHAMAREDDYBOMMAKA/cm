import {
  BarChart3,
  Copy,
  FileJson,
  FingerprintPattern,
  GitBranch,
  ListChecks,
  ScanText,
  ScrollText,
} from "lucide-react";

const features = [
  {
    icon: ScanText,
    title: "Document extraction",
    description:
      "A vision model reads each document and returns structured fields with per-field confidence, plus inline correction when a field needs a human eye.",
  },
  {
    icon: Copy,
    title: "Duplicate detection",
    description:
      "Catches exact repeats via SHA-256, near-identical images via perceptual hashing, reused invoice numbers, and semantically similar claims via embeddings.",
  },
  {
    icon: FingerprintPattern,
    title: "Document forensics",
    description:
      "Inspects EXIF and PDF metadata for signs of editing software, and flags documents modified after their claimed creation date.",
  },
  {
    icon: ListChecks,
    title: "Consistency checks",
    description:
      "Cross-checks line items against the claimed total, dates against the treatment window, procedure against diagnosis, and pricing against tariff bands.",
  },
  {
    icon: GitBranch,
    title: "Decision engine",
    description:
      "Every decision is stored with its reasons. Clean claims approve automatically; the rest are held or escalated for human review.",
  },
  {
    icon: ScrollText,
    title: "Tamper-evident audit trail",
    description:
      "Every action in a claim's lifecycle is hash-chained and append-only, with a seal that can be independently verified.",
  },
  {
    icon: FileJson,
    title: "NHCX-ready output",
    description: "Claim data can be exported as FHIR R4 resources, ready for India's NHCX exchange.",
  },
  {
    icon: BarChart3,
    title: "Metrics console",
    description:
      "Tracks straight-through-processing rate and leakage caught, so a claims team can see the pipeline earning its keep.",
  },
];

export default function Features() {
  return (
    <section id="features" className="bg-canvas py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">
            Everything a claims desk needs to trust a decision
          </h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Extraction, fraud detection, decisioning and audit — built as one pipeline, not bolted
            together.
          </p>
        </div>

        <div className="mt-16 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {features.map(({ icon: Icon, title, description }) => (
            <div
              key={title}
              className="rounded-2xl border border-border bg-surface p-6 transition-shadow hover:shadow-sm"
            >
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
