"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";

export default function DeleteDocumentButton({
  claimId,
  documentId,
}: {
  claimId: string;
  documentId: string;
}) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);

  const remove = async () => {
    if (deleting) {
      return;
    }
    setDeleting(true);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/documents/${documentId}`), {
        method: "DELETE",
      });
      if (response.ok) {
        router.refresh();
      }
    } finally {
      setDeleting(false);
    }
  };

  return (
    <button
      type="button"
      onClick={remove}
      disabled={deleting}
      aria-label="Delete document"
      className="inline-flex size-8 shrink-0 items-center justify-center rounded-lg text-muted transition-colors hover:bg-danger-soft hover:text-danger disabled:opacity-60"
    >
      <Trash2 className="size-4" />
    </button>
  );
}
