import { CircleCheck, CircleDashed, Scale } from "lucide-react";
import { isApproved, outcomeLabel, OUTCOME_STYLES, type Decision } from "@/lib/decision";
import ReviewActions from "./ReviewActions";

export default function DecisionPanel({ claimId, decision }: { claimId: string; decision?: Decision | null }) {
  if (!decision || decision.outcome === "PENDING") {
    return (
      <div className="mt-4 flex items-center gap-3 rounded-xl border border-border bg-surface px-5 py-4 text-sm text-muted">
        <CircleDashed className="size-4 shrink-0" />
        No decision yet. One is made automatically once a document has been read and checked.
      </div>
    );
  }

  const approved = isApproved(decision.outcome);

  return (
    <div className="mt-4 overflow-hidden rounded-xl border border-border bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
        <div className="flex items-center gap-3">
          {approved ? (
            <CircleCheck className="size-5 shrink-0 text-success" />
          ) : (
            <Scale className="size-5 shrink-0 text-warning" />
          )}
          <div>
            <p className="text-sm font-semibold text-ink">{outcomeLabel(decision.outcome)}</p>
            <p className="mt-0.5 text-xs text-secondary">
              {decision.automatic ? "Decided by the engine" : `Decided by ${decision.decidedBy}`}
              {decision.decidedAt ? ` · ${new Date(decision.decidedAt).toLocaleString()}` : ""}
            </p>
          </div>
        </div>
        <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${OUTCOME_STYLES[decision.outcome]}`}>
          {decision.automatic ? "AUTOMATIC" : "REVIEWED"}
        </span>
      </div>

      {decision.reasons.length > 0 ? (
        <ul className="space-y-1.5 border-t border-border px-5 py-3">
          {decision.reasons.map((reason, index) => (
            <li key={index} className="flex gap-2 text-sm text-secondary">
              <span className="text-subtle">·</span>
              {reason}
            </li>
          ))}
        </ul>
      ) : null}

      <ReviewActions claimId={claimId} />
    </div>
  );
}
