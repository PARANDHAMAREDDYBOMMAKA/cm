import { AlertTriangle, LogIn } from "lucide-react";
import { signIn } from "@/lib/auth";
import type { LoadFailure } from "@/lib/backend";

export default function LoadError({ kind, what }: { kind: LoadFailure; what: string }) {
  if (kind === "backend") {
    return (
      <div className="mt-6 flex items-start gap-3 rounded-xl border border-border bg-surface p-6 text-sm text-danger">
        <AlertTriangle className="mt-0.5 size-4 shrink-0" />
        <span>Could not load {what}. The backend is not reachable.</span>
      </div>
    );
  }

  return (
    <div className="mt-6 flex flex-col items-start gap-3 rounded-xl border border-warning bg-warning-soft p-6">
      <div className="flex items-start gap-3 text-sm text-warning">
        <AlertTriangle className="mt-0.5 size-4 shrink-0" />
        <span>Your session expired. Sign in again to continue.</span>
      </div>
      <form
        action={async () => {
          "use server";
          await signIn("zitadel", { redirectTo: "/dashboard/claims" });
        }}
      >
        <button
          type="submit"
          className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
        >
          <LogIn className="size-4" />
          Sign in again
        </button>
      </form>
    </div>
  );
}
