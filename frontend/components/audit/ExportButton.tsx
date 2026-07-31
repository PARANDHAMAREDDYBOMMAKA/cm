"use client";

import { useState } from "react";
import { Download, Loader2 } from "lucide-react";
import { proxyUrl } from "@/lib/api";

export default function ExportButton() {
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const download = async () => {
    setDownloading(true);
    setError(null);
    try {
      const response = await fetch(proxyUrl("/api/audit/export"));
      if (!response.ok) {
        setError(`Could not export the audit trail (${response.status}).`);
        return;
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "claimguard-audit.csv";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError("Could not export the audit trail. Is the backend running?");
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="flex flex-col items-end gap-1.5">
      <button
        type="button"
        onClick={download}
        disabled={downloading}
        className="inline-flex items-center gap-2 rounded-lg border border-border px-3.5 py-2 text-sm font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink disabled:opacity-60"
      >
        {downloading ? <Loader2 className="size-4 animate-spin" /> : <Download className="size-4" />}
        Export CSV
      </button>
      {error ? <p className="text-xs text-danger">{error}</p> : null}
    </div>
  );
}
