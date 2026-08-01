import { FileSearch } from "lucide-react";
import { formatAmount, type Extraction } from "@/lib/extraction";

const DASH = "—";

function stay(extraction: Extraction): string {
  const from = extraction.admissionDate;
  const to = extraction.dischargeDate;
  if (!from && !to) {
    return DASH;
  }
  if (from && to) {
    return from === to ? from : `${from} → ${to}`;
  }
  return from ?? to ?? DASH;
}

export default function ClaimFacts({ extraction }: { extraction: Extraction | null }) {
  if (!extraction || extraction.status !== "COMPLETED") {
    return (
      <div className="mt-4 flex items-center gap-3 rounded-xl border border-border bg-surface px-5 py-4 text-sm text-muted">
        <FileSearch className="size-4 shrink-0" />
        Nothing read yet. Upload a document and its details will appear here automatically.
      </div>
    );
  }

  const facts = [
    { label: "Patient", value: extraction.patientName ?? DASH },
    { label: "Provider", value: extraction.providerName ?? DASH },
    { label: "Diagnosis", value: extraction.diagnosis ?? DASH },
    { label: "Invoice no.", value: extraction.invoiceNumber ?? DASH },
    { label: "Stay", value: stay(extraction) },
    { label: "Billed", value: formatAmount(extraction.totalAmount, extraction.currency) },
  ];

  return (
    <dl className="mt-4 grid grid-cols-1 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-2 lg:grid-cols-3">
      {facts.map((fact) => (
        <div key={fact.label} className="bg-surface px-5 py-3">
          <dt className="text-xs uppercase tracking-wide text-subtle">{fact.label}</dt>
          <dd className="mt-1 break-words text-sm font-medium text-ink">{fact.value}</dd>
        </div>
      ))}
    </dl>
  );
}
