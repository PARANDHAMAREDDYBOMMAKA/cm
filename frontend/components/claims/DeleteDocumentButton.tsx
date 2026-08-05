"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import { networkMessage, problemMessage } from "@/lib/problem";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import { useToast } from "@/components/ui/Toast";

export default function DeleteDocumentButton({
  claimId,
  documentId,
  filename,
}: {
  claimId: string;
  documentId: string;
  filename?: string;
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
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/documents/${documentId}`), {
        method: "DELETE",
      });
      if (!response.ok) {
        setError(await problemMessage(response, "Could not delete this document"));
        return;
      }
      setOpen(false);
      notify({ tone: "success", title: "Document deleted", description: filename });
      router.refresh();
    } catch {
      setError(networkMessage("Could not delete this document."));
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
        aria-label={filename ? `Delete ${filename}` : "Delete document"}
        className="inline-flex size-8 shrink-0 items-center justify-center rounded-lg text-muted transition-colors hover:bg-danger-soft hover:text-danger"
      >
        <Trash2 className="size-4" />
      </button>

      <ConfirmDialog
        open={open}
        title="Delete this document?"
        description={`${
          filename ? `“${filename}”` : "This document"
        } and everything read from it will be permanently removed. This cannot be undone.`}
        confirmLabel="Delete document"
        busyLabel="Deleting…"
        busy={deleting}
        error={error}
        onConfirm={remove}
        onCancel={() => setOpen(false)}
      />
    </>
  );
}
