export type DecisionOutcome =
  | "PENDING"
  | "AUTO_APPROVED"
  | "NEEDS_REVIEW"
  | "APPROVED"
  | "FLAGGED"
  | "ESCALATED";

export type Decision = {
  outcome: DecisionOutcome;
  automatic: boolean;
  decidedBy: string | null;
  note: string | null;
  reasons: string[];
  decidedAt: string | null;
};

export type ReviewAction = "APPROVE" | "FLAG" | "ESCALATE";

export const OUTCOME_STYLES: Record<DecisionOutcome, string> = {
  PENDING: "bg-canvas text-muted",
  AUTO_APPROVED: "bg-success-soft text-success",
  NEEDS_REVIEW: "bg-warning-soft text-warning",
  APPROVED: "bg-success-soft text-success",
  FLAGGED: "bg-danger-soft text-danger",
  ESCALATED: "bg-danger-soft text-danger",
};

const OUTCOME_LABELS: Record<DecisionOutcome, string> = {
  PENDING: "Awaiting decision",
  AUTO_APPROVED: "Auto-approved",
  NEEDS_REVIEW: "Needs review",
  APPROVED: "Approved by reviewer",
  FLAGGED: "Held by reviewer",
  ESCALATED: "Escalated",
};

export function outcomeLabel(outcome: DecisionOutcome): string {
  return OUTCOME_LABELS[outcome] ?? outcome;
}

export function isApproved(outcome: DecisionOutcome): boolean {
  return outcome === "AUTO_APPROVED" || outcome === "APPROVED";
}

export function needsAttention(outcome: DecisionOutcome): boolean {
  return outcome === "NEEDS_REVIEW" || outcome === "FLAGGED" || outcome === "ESCALATED";
}
