"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Check, Loader2, Pencil, X } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import {
  confidenceOf,
  errorMessage,
  fieldValue,
  formatAmount,
  LOW_CONFIDENCE,
  type Extraction,
  type ExtractionFieldKey,
} from "@/lib/extraction";
import ConfidenceDot from "./ConfidenceDot";

type Props = {
  claimId: string;
  documentId: string;
  extraction: Extraction;
  fieldKey: ExtractionFieldKey;
  label: string;
};

export default function ExtractionField({ claimId, documentId, extraction, fieldKey, label }: Props) {
  const router = useRouter();
  const raw = fieldValue(extraction, fieldKey);
  const confidence = confidenceOf(extraction, fieldKey);
  const edited = extraction.editedFields.includes(fieldKey);

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(raw);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const needsReview = !edited && confidence != null && confidence < LOW_CONFIDENCE;

  const display =
    fieldKey === "totalAmount"
      ? formatAmount(extraction.totalAmount, extraction.currency)
      : raw || "—";

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(
        proxyUrl(`/api/claims/${claimId}/documents/${documentId}/extraction`),
        {
          method: "PATCH",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ fields: { [fieldKey]: draft } }),
        },
      );
      if (!response.ok) {
        setError(await errorMessage(response, "Save failed"));
        return;
      }
      setEditing(false);
      router.refresh();
    } catch {
      setError("Save failed. Is the backend running?");
    } finally {
      setSaving(false);
    }
  };

  const cancel = () => {
    setDraft(raw);
    setEditing(false);
    setError(null);
  };

  return (
    <div
      className={`rounded-lg border px-3 py-2 ${
        needsReview ? "border-warning bg-warning-soft" : "border-border bg-surface"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] uppercase tracking-wide text-subtle">{label}</span>
        <ConfidenceDot value={confidence} edited={edited} />
      </div>

      {editing ? (
        <div className="mt-1.5">
          <input
            autoFocus
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                save();
              }
              if (event.key === "Escape") {
                cancel();
              }
            }}
            className="w-full rounded-md border border-border-strong bg-canvas px-2 py-1 text-sm text-ink outline-none focus:border-brand"
          />
          <div className="mt-2 flex items-center gap-1.5">
            <button
              type="button"
              onClick={save}
              disabled={saving}
              className="inline-flex items-center gap-1 rounded-md bg-brand px-2 py-1 text-xs font-medium text-white hover:bg-brand-hover disabled:opacity-60"
            >
              {saving ? <Loader2 className="size-3 animate-spin" /> : <Check className="size-3" />}
              Save
            </button>
            <button
              type="button"
              onClick={cancel}
              disabled={saving}
              className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs text-secondary hover:bg-canvas"
            >
              <X className="size-3" />
              Cancel
            </button>
          </div>
          {error ? <p className="mt-1.5 text-[11px] text-danger">{error}</p> : null}
        </div>
      ) : (
        <button
          type="button"
          onClick={() => {
            setDraft(raw);
            setEditing(true);
          }}
          className="group mt-1 flex w-full items-start gap-1.5 text-left"
        >
          <span className="flex-1 break-words text-sm text-ink">{display}</span>
          <Pencil className="mt-0.5 size-3 shrink-0 text-subtle opacity-0 transition-opacity group-hover:opacity-100" />
        </button>
      )}
    </div>
  );
}
