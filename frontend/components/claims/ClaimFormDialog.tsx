"use client";

import { useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Loader2, Pencil, Plus } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import { networkMessage, problemMessage } from "@/lib/problem";
import Modal from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

const STATUSES = ["RECEIVED", "PROCESSING", "APPROVED", "FLAGGED", "ESCALATED"];

const REFERENCE_PATTERN = /^[\p{Alphabetic}\p{Nd}][\p{Alphabetic}\p{Nd} ._/-]*$/u;

const inputClass =
  "w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-brand";
const primaryButton =
  "inline-flex items-center gap-2 rounded-lg bg-brand px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:opacity-60";
const secondaryButton =
  "inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink";
const ghostButton =
  "rounded-lg px-3.5 py-2 text-sm font-medium text-secondary transition-colors hover:bg-canvas disabled:opacity-60";

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
  const { notify } = useToast();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reference, setReference] = useState(initial?.reference ?? "");
  const [claimantName, setClaimantName] = useState(initial?.claimantName ?? "");
  const [note, setNote] = useState(initial?.note ?? "");
  const [status, setStatus] = useState(initial?.status ?? "RECEIVED");

  const reset = () => {
    setReference(initial?.reference ?? "");
    setClaimantName(initial?.claimantName ?? "");
    setNote(initial?.note ?? "");
    setStatus(initial?.status ?? "RECEIVED");
    setError(null);
  };

  const close = () => {
    if (saving) {
      return;
    }
    setOpen(false);
    reset();
  };

  const validate = (): string | null => {
    const trimmed = reference.trim();
    if (trimmed.length > 64) {
      return "Reference must be at most 64 characters.";
    }
    if (trimmed.length > 0 && !REFERENCE_PATTERN.test(trimmed)) {
      return "Reference may only contain letters, digits, spaces and . _ / -";
    }
    if (claimantName.trim().length > 255) {
      return "Claimant name must be at most 255 characters.";
    }
    if (note.trim().length > 2000) {
      return "Note must be at most 2000 characters.";
    }
    return null;
  };

  const submit = async () => {
    const invalid = validate();
    if (invalid) {
      setError(invalid);
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const base = {
        reference: reference.trim(),
        claimantName: claimantName.trim(),
        note: note.trim(),
      };
      const body = mode === "create" ? base : { ...base, status };
      const url = mode === "create" ? proxyUrl("/api/claims") : proxyUrl(`/api/claims/${claimId}`);
      const response = await fetch(url, {
        method: mode === "create" ? "POST" : "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        setError(
          await problemMessage(
            response,
            mode === "create" ? "Could not create the claim" : "Could not save your changes",
          ),
        );
        return;
      }
      if (mode === "create") {
        const claim = await response.json();
        setOpen(false);
        reset();
        notify({ tone: "success", title: "Claim created", description: claim.reference });
        router.push(`/dashboard/claims/${claim.id}`);
        router.refresh();
      } else {
        setOpen(false);
        notify({ tone: "success", title: "Claim updated" });
        router.refresh();
      }
    } catch {
      setError(
        networkMessage(
          mode === "create" ? "Could not create the claim." : "Could not save your changes.",
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={() => {
          reset();
          setOpen(true);
        }}
        className={mode === "create" ? primaryButton : secondaryButton}
      >
        {mode === "create" ? <Plus className="size-4" /> : <Pencil className="size-4" />}
        {mode === "create" ? "New claim" : "Edit"}
      </button>

      <Modal
        open={open}
        onClose={close}
        title={mode === "create" ? "New claim" : "Edit claim"}
        description={
          mode === "create"
            ? "Create a claim, then upload its documents."
            : "Update this claim's details."
        }
        footer={
          <>
            <button type="button" onClick={close} disabled={saving} className={ghostButton}>
              Cancel
            </button>
            <button type="button" onClick={submit} disabled={saving} className={primaryButton}>
              {saving ? <Loader2 className="size-4 animate-spin" /> : null}
              {saving ? "Saving…" : mode === "create" ? "Create claim" : "Save changes"}
            </button>
          </>
        }
      >
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
        >
          <Field label="Reference" hint="Blank = auto-generate">
            <input
              value={reference}
              onChange={(event) => setReference(event.target.value)}
              maxLength={64}
              className={inputClass}
              placeholder="CLM-…"
            />
          </Field>
          <Field label="Claimant name">
            <input
              value={claimantName}
              onChange={(event) => setClaimantName(event.target.value)}
              maxLength={255}
              className={inputClass}
              placeholder="Full name"
            />
          </Field>
          <Field label="Note" hint={`${note.length}/2000`}>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              rows={3}
              maxLength={2000}
              className={inputClass}
              placeholder="Optional details"
            />
          </Field>
          {mode === "edit" ? (
            <Field label="Status">
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                className={inputClass}
              >
                {STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </Field>
          ) : null}
          {error ? (
            <p role="alert" className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">
              {error}
            </p>
          ) : null}
          <button type="submit" className="hidden" aria-hidden tabIndex={-1} />
        </form>
      </Modal>
    </>
  );
}
