"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";

export default function DeleteClaimButton({ claimId }: { claimId: string }) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const remove = async () => {
    setDeleting(true);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}`), { method: "DELETE" });
      if (response.ok) {
        router.push("/dashboard/claims");
        router.refresh();
      }
    } finally {
      setDeleting(false);
    }
  };

  if (confirming) {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="text-sm text-muted">Delete?</span>
        <button
          type="button"
          onClick={remove}
          disabled={deleting}
          className="rounded-lg bg-danger px-3 py-1.5 text-xs font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-60"
        >
          {deleting ? "Deleting…" : "Yes, delete"}
        </button>
        <button
          type="button"
          onClick={() => setConfirming(false)}
          className="rounded-lg border border-border px-3 py-1.5 text-xs text-secondary transition-colors hover:bg-canvas"
        >
          Cancel
        </button>
      </span>
    );
  }

  return (
    <button
      type="button"
      onClick={() => setConfirming(true)}
      className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-danger transition-colors hover:bg-danger-soft"
    >
      <Trash2 className="size-4" />
      Delete
    </button>
  );
}
