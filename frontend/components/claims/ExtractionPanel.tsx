"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle, Loader2, RefreshCw, Sparkles } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import {
  errorMessage,
  EXTRACTION_FIELDS,
  formatAmount,
  isRunning,
  type Extraction,
} from "@/lib/extraction";
import ExtractionField from "./ExtractionField";

type Props = {
  claimId: string;
  documentId: string;
  extraction: Extraction | null;
};

export default function ExtractionPanel({ claimId, documentId, extraction }: Props) {
  const router = useRouter();
  const [requesting, setRequesting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const rerun = async () => {
    setRequesting(true);
    setError(null);
    try {
      const response = await fetch(
        proxyUrl(`/api/claims/${claimId}/documents/${documentId}/extraction`),
        { method: "POST" },
      );
      if (!response.ok) {
        setError(await errorMessage(response, "Could not start extraction"));
        return;
      }
      router.refresh();
    } catch {
      setError("Could not start extraction. Is the backend running?");
    } finally {
      setRequesting(false);
    }
  };

  const running = isRunning(extraction);

  return (
    <div className="border-t border-border bg-canvas px-5 py-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Sparkles className="size-4 text-brand" />
          <span className="text-sm font-medium text-ink">Extracted data</span>
          {extraction?.model ? (
            <span className="rounded-full bg-surface px-2 py-0.5 text-[11px] text-subtle">
              {extraction.model}
            </span>
          ) : null}
        </div>
        <button
          type="button"
          onClick={rerun}
          disabled={requesting || running}
          className="inline-flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-secondary transition-colors hover:bg-surface hover:text-ink disabled:opacity-60"
        >
          {requesting ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
          {extraction ? "Re-run" : "Extract"}
        </button>
      </div>

      {error ? <p className="mt-2 text-xs text-danger">{error}</p> : null}

      {!extraction ? (
        <p className="mt-3 text-sm text-muted">Not read yet.</p>
      ) : running ? (
        <div className="mt-3 flex items-center gap-2 text-sm text-secondary">
          <Loader2 className="size-4 animate-spin text-brand" />
          Reading the document…
        </div>
      ) : extraction.status === "SKIPPED" ? (
        <p className="mt-3 text-sm text-muted">
          No document reader is configured. Set GEMINI_API_KEY on the backend to enable extraction.
        </p>
      ) : extraction.status === "FAILED" ? (
        <div className="mt-3 flex items-start gap-2 rounded-lg border border-danger bg-danger-soft px-3 py-2 text-sm text-danger">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span className="break-words">{extraction.error ?? "Extraction failed."}</span>
        </div>
      ) : (
        <>
          <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {EXTRACTION_FIELDS.map((field) => (
              <ExtractionField
                key={field.key}
                claimId={claimId}
                documentId={documentId}
                extraction={extraction}
                fieldKey={field.key}
                label={field.label}
              />
            ))}
          </div>

          {extraction.lineItems.length > 0 ? (
            <div className="mt-4 overflow-x-auto rounded-lg border border-border bg-surface">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-[11px] uppercase tracking-wide text-subtle">
                    <th className="px-3 py-2 font-medium">Particulars</th>
                    <th className="px-3 py-2 font-medium">Code</th>
                    <th className="px-3 py-2 text-right font-medium">Qty</th>
                    <th className="px-3 py-2 text-right font-medium">Rate</th>
                    <th className="px-3 py-2 text-right font-medium">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {extraction.lineItems.map((item) => (
                    <tr key={item.id} className="border-b border-border last:border-0">
                      <td className="px-3 py-2 text-ink">{item.description ?? "—"}</td>
                      <td className="px-3 py-2 text-secondary">{item.code ?? "—"}</td>
                      <td className="px-3 py-2 text-right tabular-nums text-secondary">
                        {item.quantity ?? "—"}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums text-secondary">
                        {formatAmount(item.unitAmount, extraction.currency)}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums text-ink">
                        {formatAmount(item.amount, extraction.currency)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
