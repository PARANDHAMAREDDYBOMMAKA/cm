export type AuditEvent = {
  seq: number;
  id: string;
  claimId: string | null;
  claimReference: string | null;
  actor: string;
  action: string;
  summary: string;
  details: Record<string, string>;
  previousHash: string;
  hash: string;
  createdAt: string;
};

export type AuditVerification = {
  intact: boolean;
  eventCount: number;
  brokenAtSeq: number | null;
  headHash: string | null;
  message: string;
};

const ACTION_LABELS: Record<string, string> = {
  CLAIM_CREATED: "Claim created",
  CLAIM_UPDATED: "Claim edited",
  CLAIM_DELETED: "Claim deleted",
  DOCUMENT_UPLOADED: "Document uploaded",
  DOCUMENT_DELETED: "Document removed",
  EXTRACTION_COMPLETED: "Document read",
  EXTRACTION_FAILED: "Read failed",
  EXTRACTION_EDITED: "Fields corrected",
  RISK_ASSESSED: "Risk assessed",
  DECISION_RECORDED: "Decision recorded",
  REVIEW_RECORDED: "Reviewed",
  NHCX_SUBMITTED: "Sent to NHCX",
  AUDIT_EXPORTED: "Audit exported",
};

const ACTION_STYLES: Record<string, string> = {
  CLAIM_DELETED: "bg-danger-soft text-danger",
  DOCUMENT_DELETED: "bg-danger-soft text-danger",
  EXTRACTION_FAILED: "bg-danger-soft text-danger",
  RISK_ASSESSED: "bg-warning-soft text-warning",
  DECISION_RECORDED: "bg-brand-soft text-brand",
  REVIEW_RECORDED: "bg-brand-soft text-brand",
};

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action.replaceAll("_", " ").toLowerCase();
}

export function actionStyle(action: string): string {
  return ACTION_STYLES[action] ?? "bg-canvas text-muted";
}

export function shortHash(hash: string): string {
  return hash.slice(0, 10);
}
