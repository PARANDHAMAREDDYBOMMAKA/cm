import { FileSearch, TriangleAlert } from "lucide-react";
import { consolidate } from "@/lib/consolidate";
import { formatAmount, type Extraction } from "@/lib/extraction";

const DASH = "—";

type Source = {
  filename: string;
  extraction: Extraction | null;
};

function display(key: string, value: string, currency: string | null): string {
  if (key === "totalAmount") {
    return formatAmount(value, currency);
  }
  return value;
}

export default function ConsolidatedFacts({ documents }: { documents: Source[] }) {
  const { fields, sources, conflicts } = consolidate(documents);

  if (sources === 0) {
    return (
      <div className="mt-4 flex items-center gap-3 rounded-xl border border-border bg-surface px-5 py-4 text-sm text-muted">
        <FileSearch className="size-4 shrink-0" />
        Nothing read yet. Upload a document and its details will appear here automatically.
      </div>
    );
  }

  const currency =
    documents.find((document) => document.extraction?.currency)?.extraction?.currency ?? null;
  const shown = fields.filter((field) => !field.missing);

  return (
    <div className="mt-4 overflow-hidden rounded-xl border border-border bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-5 py-3">
        <div>
          <p className="text-sm font-semibold text-ink">Consolidated details</p>
          <p className="mt-0.5 text-xs text-secondary">
            Merged from {sources} {sources === 1 ? "document" : "documents"}.
          </p>
        </div>
        {conflicts > 0 ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-warning-soft px-2.5 py-1 text-xs font-medium text-warning">
            <TriangleAlert className="size-3.5" />
            {conflicts} {conflicts === 1 ? "field disagrees" : "fields disagree"}
          </span>
        ) : sources > 1 ? (
          <span className="rounded-full bg-success-soft px-2.5 py-1 text-xs font-medium text-success">
            All documents agree
          </span>
        ) : null}
      </div>

      <dl className="grid grid-cols-1 gap-px bg-border sm:grid-cols-2 lg:grid-cols-3">
        {shown.map((field) => (
          <div key={field.key} className="bg-surface px-5 py-3">
            <dt className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-subtle">
              {field.label}
              {!field.agreed ? <TriangleAlert className="size-3 text-warning" /> : null}
            </dt>

            {field.agreed ? (
              <dd className="mt-1 break-words text-sm font-medium text-ink">
                {display(field.key, field.values[0]?.value ?? DASH, currency)}
                {sources > 1 && field.values[0] ? (
                  <span className="ml-1.5 text-xs font-normal text-subtle">
                    ({field.values[0].documents.length}/{sources})
                  </span>
                ) : null}
              </dd>
            ) : (
              <dd className="mt-1 space-y-1.5">
                {field.values.map((entry) => (
                  <div key={entry.value}>
                    <p className="break-words text-sm font-medium text-warning">
                      {display(field.key, entry.value, currency)}
                    </p>
                    <p className="truncate text-xs text-subtle" title={entry.documents.join(", ")}>
                      {entry.documents.join(", ")}
                    </p>
                  </div>
                ))}
              </dd>
            )}
          </div>
        ))}
      </dl>
    </div>
  );
}
