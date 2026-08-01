import { CheckCircle2, FileStack, ScanEye, SlidersHorizontal, UploadCloud } from "lucide-react";

const pipeline = [
  {
    icon: UploadCloud,
    title: "Upload",
    description: "Drag and drop multiple files at once. Documents land in Cloudflare R2, metadata in Postgres.",
  },
  {
    icon: FileStack,
    title: "Rasterise",
    description:
      "PDFs are rendered to images with PDFBox first, since both readers below are image-only.",
  },
  {
    icon: ScanEye,
    title: "Read",
    description:
      "Cloudflare Workers AI (llama-4-scout) reads the page, with an automatic fallback to Groq if it can't.",
  },
  {
    icon: SlidersHorizontal,
    title: "Score & correct",
    description: "Every field comes back with a confidence score. Low-confidence fields are highlighted for review.",
  },
];

const fields = [
  "Document type",
  "Patient name",
  "Patient age",
  "Patient gender",
  "Patient ID",
  "Provider name",
  "Provider address",
  "Diagnosis",
  "Procedures",
  "Admission date",
  "Discharge date",
  "Invoice number",
  "Invoice date",
  "Total amount",
  "Currency",
  "Line items — description, code, quantity, unit rate, amount",
];

export default function ExtractionSection() {
  return (
    <section id="extraction" className="border-b border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Intake & extraction</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Upload the documents once. A vision language model reads them the way a claims
            examiner would, and returns structured, checkable fields instead of a wall of text.
          </p>
        </div>

        <div className="relative mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <div className="absolute inset-x-0 top-9 hidden h-px bg-border lg:block" aria-hidden="true" />
          {pipeline.map(({ icon: Icon, title, description }, index) => (
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

        <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-5">
          <div className="rounded-2xl border border-border bg-canvas p-6 lg:col-span-3">
            <h3 className="text-base font-semibold text-ink">Fields extracted from every document</h3>
            <p className="mt-1 text-sm leading-relaxed text-secondary">
              Each field carries its own confidence score, and any field can be corrected inline.
              Corrections are remembered and never overwritten by a re-run.
            </p>
            <ul className="mt-5 flex flex-wrap gap-2">
              {fields.map((field) => (
                <li
                  key={field}
                  className="rounded-full border border-border bg-surface px-3 py-1.5 text-xs font-medium text-secondary"
                >
                  {field}
                </li>
              ))}
            </ul>
          </div>

          <div className="flex flex-col justify-between rounded-2xl border border-brand/20 bg-brand-soft p-6 lg:col-span-2">
            <div>
              <div className="flex size-10 items-center justify-center rounded-lg bg-surface text-brand">
                <CheckCircle2 className="size-5" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-ink">Verified against a real bill</h3>
              <p className="mt-2 text-sm leading-relaxed text-secondary">
                16 of 16 fields plus all 8 line items were extracted correctly from a synthetic
                Indian hospital bill, including Indian lakh digit grouping such as 1,40,000.
              </p>
            </div>
            <p className="mt-6 text-xs text-muted">
              v1 demo, measured against a synthetic claims set — not production volume figures.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
