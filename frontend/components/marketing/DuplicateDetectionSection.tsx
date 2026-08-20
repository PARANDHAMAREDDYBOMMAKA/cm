import { Copy, Hash, Images, Repeat } from "lucide-react";

const signals = [
  {
    icon: Hash,
    name: "Exact duplicate",
    catches: "The SHA-256 hash of the file content matches a document already on file.",
  },
  {
    icon: Images,
    name: "Near-identical image",
    catches:
      "A perceptual hash (dHash) within a Hamming distance of 6 flags a re-scanned, re-photographed or lightly edited copy — for images directly, and for PDFs via the rasteriser.",
  },
  {
    icon: Repeat,
    name: "Reused invoice number",
    catches: "The same invoice number shows up on a different claim.",
  },
  {
    icon: Copy,
    name: "Semantic duplicate",
    catches:
      "Cloudflare bge embeddings compared with pgvector cosine similarity at 0.94 or above catch the same claim re-typed or reworded, not just re-uploaded.",
  },
];

export default function DuplicateDetectionSection() {
  return (
    <section id="duplicates" className="border-b border-border bg-canvas py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Duplicate detection</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Four independent checks catch the same claim coming back around, whatever form it
            comes back in.
          </p>
        </div>

        <dl className="mt-12 grid grid-cols-1 gap-5 sm:grid-cols-2">
          {signals.map(({ icon: Icon, name, catches }) => (
            <div key={name} className="rounded-2xl border border-border bg-surface p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-brand">
                  <Icon className="size-4.5" />
                </div>
                <dt className="text-base font-semibold text-ink">{name}</dt>
              </div>
              <dd className="mt-3 text-sm leading-relaxed text-secondary">{catches}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
