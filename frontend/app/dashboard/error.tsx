"use client";

import { useEffect } from "react";
import { AlertTriangle, RotateCcw } from "lucide-react";

export default function DashboardError({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mt-6 rounded-xl border border-danger bg-danger-soft p-6">
        <div className="flex items-start gap-3 text-danger">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div className="min-w-0">
            <h2 className="text-base font-semibold">Something went wrong</h2>
            <p className="mt-1 text-sm">
              This page could not be rendered. The rest of the console still works.
            </p>
            {error.digest ? (
              <p className="mt-2 font-mono text-xs opacity-80">Reference: {error.digest}</p>
            ) : null}
          </div>
        </div>
        <button
          type="button"
          onClick={() => unstable_retry()}
          className="mt-5 inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
        >
          <RotateCcw className="size-4" />
          Try again
        </button>
      </div>
    </div>
  );
}
