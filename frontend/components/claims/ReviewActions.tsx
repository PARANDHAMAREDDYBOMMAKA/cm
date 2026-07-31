"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowUpRight, Check, Flag, Loader2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import type { ReviewAction } from "@/lib/decision";

const ACTIONS: { action: ReviewAction; label: string; icon: typeof Check; className: string }[] = [
  {
    action: "APPROVE",
    label: "Approve",
    icon: Check,
    className: "border-success text-success hover:bg-success-soft",
  },
  {
    action: "FLAG",
    label: "Hold",
    icon: Flag,
    className: "border-warning text-warning hover:bg-warning-soft",
  },
  {
    action: "ESCALATE",
    label: "Escalate",
    icon: ArrowUpRight,
    className: "border-danger text-danger hover:bg-danger-soft",
  },
];

export default function ReviewActions({ claimId, settled }: { claimId: string; settled: boolean }) {
  const router = useRouter();
  const [note, setNote] = useState("");
  const [pending, setPending] = useState<ReviewAction | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(!settled);

  const submit = async (action: ReviewAction) => {
    setPending(action);
    setError(null);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/review`), {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ action, note }),
      });
      if (!response.ok) {
        setError(`Could not record the review (${response.status}).`);
        return;
      }
      setNote("");
      setOpen(false);
      router.refresh();
    } catch {
      setError("Could not record the review. Is the backend running?");
    } finally {
      setPending(null);
    }
  };

  if (!open) {
    return (
      <div className="border-t border-border px-5 py-3">
        <button
          type="button"
          onClick={() => setOpen(true)}
          className="text-sm text-secondary underline-offset-4 transition-colors hover:text-ink hover:underline"
        >
          Change this decision
        </button>
      </div>
    );
  }

  return (
    <div className="border-t border-border px-5 py-4">
      <p className="text-xs font-medium uppercase tracking-wide text-subtle">Reviewer decision</p>
      <textarea
        value={note}
        onChange={(event) => setNote(event.target.value)}
        rows={2}
        placeholder="Why are you making this call? (optional)"
        className="mt-2 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none transition-colors focus:border-brand"
      />
      <div className="mt-3 flex flex-wrap gap-2">
        {ACTIONS.map(({ action, label, icon: Icon, className }) => (
          <button
            key={action}
            type="button"
            onClick={() => submit(action)}
            disabled={pending !== null}
            className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-60 ${className}`}
          >
            {pending === action ? <Loader2 className="size-4 animate-spin" /> : <Icon className="size-4" />}
            {label}
          </button>
        ))}
      </div>
      {error ? <p className="mt-2 text-xs text-danger">{error}</p> : null}
      {settled ? (
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="mt-3 text-xs text-muted underline-offset-4 transition-colors hover:text-ink hover:underline"
        >
          Cancel
        </button>
      ) : null}
    </div>
  );
}
