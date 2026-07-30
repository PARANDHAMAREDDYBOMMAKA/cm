export const STATUS_STYLES: Record<string, string> = {
  RECEIVED: "bg-brand-soft text-brand",
  PROCESSING: "bg-warning-soft text-warning",
  EXTRACTED: "bg-brand-soft text-brand",
  FAILED: "bg-danger-soft text-danger",
  APPROVED: "bg-success-soft text-success",
  FLAGGED: "bg-danger-soft text-danger",
  ESCALATED: "bg-danger-soft text-danger",
};

export function statusStyle(status: string): string {
  return STATUS_STYLES[status] ?? "bg-canvas text-muted";
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
