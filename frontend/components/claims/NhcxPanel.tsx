"use client";

import { useState } from "react";
import { FileJson, Loader2, Send } from "lucide-react";
import { proxyUrl } from "@/lib/api";

type NhcxResult = {
  correlationId: string;
  claimReference: string;
  participantCode: string;
  status: string;
  delivered: boolean;
  bundleBytes: number;
  preparedAt: string;
  message: string;
};

export default function NhcxPanel({ claimId }: { claimId: string }) {
  const [preparing, setPreparing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<NhcxResult | null>(null);

  const prepare = async () => {
    setPreparing(true);
    setError(null);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/nhcx`), { method: "POST" });
      if (!response.ok) {
        setError(`Could not prepare the NHCX submission (${response.status}).`);
        return;
      }
      setResult(await response.json());
    } catch {
      setError("Could not prepare the NHCX submission. Is the backend running?");
    } finally {
      setPreparing(false);
    }
  };

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
        <div>
          <p className="text-sm font-semibold text-ink">NHCX</p>
          <p className="mt-0.5 text-xs text-secondary">Build the FHIR R4 bundle and prepare it for exchange.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <a
            href={proxyUrl(`/api/claims/${claimId}/fhir`)}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink"
          >
            <FileJson className="size-4" />
            View FHIR bundle
          </a>
          <button
            type="button"
            onClick={prepare}
            disabled={preparing}
            className="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:opacity-60"
          >
            {preparing ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
            Prepare NHCX submission
          </button>
        </div>
      </div>

      {error ? <p className="border-t border-border px-5 py-3 text-xs text-danger">{error}</p> : null}

      {result ? (
        <dl className="grid grid-cols-1 gap-x-4 gap-y-2 border-t border-border px-5 py-4 sm:grid-cols-2">
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Status</dt>
            <dd className="text-secondary">{result.status}</dd>
          </div>
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Delivered</dt>
            <dd className="text-secondary">{result.delivered ? "Yes" : "No"}</dd>
          </div>
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Participant code</dt>
            <dd className="text-secondary">{result.participantCode}</dd>
          </div>
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Correlation ID</dt>
            <dd className="break-all text-secondary">{result.correlationId}</dd>
          </div>
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Bundle size</dt>
            <dd className="text-secondary">{result.bundleBytes} bytes</dd>
          </div>
          <div className="flex gap-1.5 text-xs">
            <dt className="text-subtle">Prepared at</dt>
            <dd className="text-secondary">{new Date(result.preparedAt).toLocaleString()}</dd>
          </div>
          <div className="col-span-full text-sm text-secondary">{result.message}</div>
        </dl>
      ) : null}
    </div>
  );
}
