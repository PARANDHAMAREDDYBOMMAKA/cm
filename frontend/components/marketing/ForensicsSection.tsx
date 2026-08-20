import { Eraser, FileCog, FileWarning, ImageOff } from "lucide-react";

const signals = [
  {
    icon: FileCog,
    name: "Edited with known software",
    catches:
      "The file's own metadata says it was last written by Photoshop, Canva, Acrobat, iLovePDF or a similar editor.",
  },
  {
    icon: Eraser,
    name: "Metadata stripped",
    catches: "The metadata a normal scan or export would carry is missing entirely.",
  },
  {
    icon: FileWarning,
    name: "Modified after creation",
    catches: "The file's modification timestamp is later than its creation timestamp.",
  },
];

export default function ForensicsSection() {
  return (
    <section id="forensics" className="border-b border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Document forensics</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Every document is inspected for signs it was touched after it left the provider&apos;s
            system — EXIF metadata for images, document information for PDFs.
          </p>
        </div>

        <dl className="mt-12 grid grid-cols-1 gap-5 sm:grid-cols-3">
          {signals.map(({ icon: Icon, name, catches }) => (
            <div key={name} className="rounded-2xl border border-border bg-canvas p-6">
              <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
                <Icon className="size-5" />
              </div>
              <dt className="mt-4 text-base font-semibold text-ink">{name}</dt>
              <dd className="mt-2 text-sm leading-relaxed text-secondary">{catches}</dd>
            </div>
          ))}
        </dl>

        <div className="mt-6 flex items-start gap-3 rounded-2xl border border-warning/20 bg-warning-soft p-5">
          <ImageOff className="mt-0.5 size-5 shrink-0 text-warning" />
          <p className="text-sm leading-relaxed text-secondary">
            <span className="font-medium text-ink">AI-generated-image detection is not active.</span>{" "}
            The interface for it exists in the pipeline, but no free provider is currently wired in,
            so this signal is not checked today.
          </p>
        </div>
      </div>
    </section>
  );
}
