"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Pencil, Plus, X } from "lucide-react";
import { proxyUrl } from "@/lib/api";

const STATUSES = ["RECEIVED", "PROCESSING", "APPROVED", "FLAGGED", "ESCALATED"];

const inputClass =
  "w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-brand";
const primaryButton =
  "inline-flex items-center gap-2 rounded-lg bg-brand px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:opacity-60";
const secondaryButton =
  "inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink";
const ghostButton = "rounded-lg px-3.5 py-2 text-sm font-medium text-secondary transition-colors hover:bg-canvas";

type Initial = {
  reference?: string;
  claimantName?: string;
  note?: string;
  status?: string;
};

function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 flex items-baseline justify-between">
        <span className="text-sm font-medium text-secondary">{label}</span>
        {hint ? <span className="text-xs text-subtle">{hint}</span> : null}
      </span>
      {children}
    </label>
  );
}

export default function ClaimFormDialog({
  mode,
  claimId,
  initial,
}: {
  mode: "create" | "edit";
  claimId?: string;
  initial?: Initial;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reference, setReference] = useState(initial?.reference ?? "");
  const [claimantName, setClaimantName] = useState(initial?.claimantName ?? "");
  const [note, setNote] = useState(initial?.note ?? "");
  const [status, setStatus] = useState(initial?.status ?? "RECEIVED");

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  const submit = async () => {
    setSaving(true);
    setError(null);
    try {
      const body =
        mode === "create" ? { reference, claimantName, note } : { reference, claimantName, note, status };
      const url = mode === "create" ? proxyUrl("/api/claims") : proxyUrl(`/api/claims/${claimId}`);
      const response = await fetch(url, {
        method: mode === "create" ? "POST" : "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        setError(`Request failed (${response.status}).`);
        return;
      }
      if (mode === "create") {
        const claim = await response.json();
        setOpen(false);
        router.push(`/dashboard/claims/${claim.id}`);
      } else {
        setOpen(false);
        router.refresh();
      }
    } catch {
      setError("Request failed. Is the backend running?");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <button type="button" onClick={() => setOpen(true)} className={mode === "create" ? primaryButton : secondaryButton}>
        {mode === "create" ? <Plus className="size-4" /> : <Pencil className="size-4" />}
        {mode === "create" ? "New claim" : "Edit"}
      </button>

      {open ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4"
          onClick={() => setOpen(false)}
        >
          <div
            className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold tracking-tight">
                {mode === "create" ? "New claim" : "Edit claim"}
              </h2>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="text-muted transition-colors hover:text-ink"
              >
                <X className="size-5" />
              </button>
            </div>

            <div className="mt-5 space-y-4">
              <Field label="Reference" hint="Blank = auto-generate">
                <input
                  value={reference}
                  onChange={(event) => setReference(event.target.value)}
                  className={inputClass}
                  placeholder="CLM-…"
                />
              </Field>
              <Field label="Claimant name">
                <input
                  value={claimantName}
                  onChange={(event) => setClaimantName(event.target.value)}
                  className={inputClass}
                  placeholder="Full name"
                />
              </Field>
              <Field label="Note">
                <textarea
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                  rows={3}
                  className={inputClass}
                  placeholder="Optional details"
                />
              </Field>
              {mode === "edit" ? (
                <Field label="Status">
                  <select value={status} onChange={(event) => setStatus(event.target.value)} className={inputClass}>
                    {STATUSES.map((value) => (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    ))}
                  </select>
                </Field>
              ) : null}
              {error ? <p className="text-xs text-danger">{error}</p> : null}
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button type="button" onClick={() => setOpen(false)} className={ghostButton}>
                Cancel
              </button>
              <button type="button" onClick={submit} disabled={saving} className={primaryButton}>
                {saving ? "Saving…" : mode === "create" ? "Create claim" : "Save changes"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
