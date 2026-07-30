import { ShieldAlert, ShieldCheck } from "lucide-react";
import { shortHash, type AuditVerification } from "@/lib/audit";

export default function SealBadge({ verification }: { verification: AuditVerification | null }) {
  if (!verification) {
    return null;
  }

  const intact = verification.intact;

  return (
    <div
      className={`flex flex-wrap items-center gap-3 rounded-xl border px-5 py-4 ${
        intact ? "border-border bg-surface" : "border-danger bg-danger-soft"
      }`}
    >
      {intact ? (
        <ShieldCheck className="size-5 shrink-0 text-success" />
      ) : (
        <ShieldAlert className="size-5 shrink-0 text-danger" />
      )}
      <div className="min-w-0 flex-1">
        <p className={`text-sm font-semibold ${intact ? "text-ink" : "text-danger"}`}>
          {intact ? "Seal intact" : "Seal broken"}
        </p>
        <p className="mt-0.5 text-xs text-secondary">{verification.message}</p>
      </div>
      {verification.headHash ? (
        <span className="font-mono text-[11px] text-subtle">head {shortHash(verification.headHash)}</span>
      ) : null}
    </div>
  );
}
