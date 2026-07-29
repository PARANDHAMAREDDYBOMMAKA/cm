export type RiskBand = "UNASSESSED" | "CLEAN" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type FraudSignal = {
  id: string;
  documentId: string | null;
  type: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  weight: number;
  message: string;
  details: Record<string, string>;
};

export type Risk = {
  score: number;
  band: RiskBand;
  signalCount: number;
  assessedAt: string | null;
  signals: FraudSignal[];
};

export const BAND_STYLES: Record<RiskBand, string> = {
  UNASSESSED: "bg-canvas text-muted",
  CLEAN: "bg-success-soft text-success",
  LOW: "bg-success-soft text-success",
  MEDIUM: "bg-warning-soft text-warning",
  HIGH: "bg-danger-soft text-danger",
  CRITICAL: "bg-danger-soft text-danger",
};

export const SEVERITY_STYLES: Record<FraudSignal["severity"], string> = {
  LOW: "bg-canvas text-muted",
  MEDIUM: "bg-warning-soft text-warning",
  HIGH: "bg-danger-soft text-danger",
  CRITICAL: "bg-danger-soft text-danger",
};

const SIGNAL_LABELS: Record<string, string> = {
  EXACT_DUPLICATE: "Exact duplicate",
  NEAR_DUPLICATE_IMAGE: "Near-identical image",
  SEMANTIC_DUPLICATE: "Matching contents",
  REUSED_INVOICE_NUMBER: "Reused invoice number",
  EDITING_SOFTWARE: "Edited in image software",
  METADATA_STRIPPED: "Metadata stripped",
  MODIFIED_AFTER_CREATION: "Modified after creation",
  AI_GENERATED: "Likely AI generated",
  SHARED_DEVICE: "Shared device",
};

export function signalLabel(type: string): string {
  return SIGNAL_LABELS[type] ?? type.replaceAll("_", " ").toLowerCase();
}

export function isFlagged(band: RiskBand): boolean {
  return band === "HIGH" || band === "CRITICAL";
}
