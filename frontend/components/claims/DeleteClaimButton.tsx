"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import { networkMessage, problemMessage } from "@/lib/problem";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import { useToast } from "@/components/ui/Toast";

export default function DeleteClaimButton({
  claimId,
  reference,
}: {
  claimId: string;
  reference?: string;
}) {
  const router = useRouter();
  const { notify } = useToast();
  const [open, setOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const remove = async () => {
    setDeleting(true);
    setError(null);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}`), { method: "DELETE" });
      if (!response.ok) {
        setError(await problemMessage(response, "Could not delete this claim"));
        return;
      }
      setOpen(false);
      notify({
        tone: "success",
        title: "Claim deleted",
        description: reference ? `${reference} and its documents were removed.` : undefined,
      });
      router.push("/dashboard/claims");
      router.refresh();
    } catch {
      setError(networkMessage("Could not delete this claim."));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={() => {
          setError(null);
          setOpen(true);
        }}
        className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-danger transition-colors hover:bg-danger-soft"
      >
        <Trash2 className="size-4" />
        Delete
      </button>

      <ConfirmDialog
        open={open}
        title="Delete this claim?"
        description={`${
          reference ? `Claim ${reference}` : "This claim"
        } and every document, extraction and risk signal on it will be permanently removed. This cannot be undone.`}
        confirmLabel="Delete claim"
        busyLabel="Deleting…"
        busy={deleting}
        error={error}
        onConfirm={remove}
        onCancel={() => setOpen(false)}
      />
    </>
  );
}
