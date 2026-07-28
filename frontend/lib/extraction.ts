export type ExtractionStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "SKIPPED";

export type LineItem = {
  id: string;
  description: string | null;
  code: string | null;
  quantity: string | null;
  unitAmount: string | null;
  amount: string | null;
};

export type Extraction = {
  id: string;
  status: ExtractionStatus;
  model: string | null;
  documentType: string | null;
  patientName: string | null;
  patientAge: string | null;
  patientGender: string | null;
  patientId: string | null;
  providerName: string | null;
  providerAddress: string | null;
  diagnosis: string | null;
  procedures: string[];
  admissionDate: string | null;
  dischargeDate: string | null;
  invoiceNumber: string | null;
  invoiceDate: string | null;
  totalAmount: string | null;
  currency: string | null;
  lineItems: LineItem[];
  confidence: Record<string, number>;
  editedFields: string[];
  error: string | null;
  updatedAt: string;
};

export type ExtractionFieldKey =
  | "documentType"
  | "patientName"
  | "patientAge"
  | "patientGender"
  | "patientId"
  | "providerName"
  | "providerAddress"
  | "diagnosis"
  | "procedures"
  | "admissionDate"
  | "dischargeDate"
  | "invoiceNumber"
  | "invoiceDate"
  | "totalAmount"
  | "currency";

export const EXTRACTION_FIELDS: { key: ExtractionFieldKey; label: string }[] = [
  { key: "documentType", label: "Document type" },
  { key: "patientName", label: "Patient" },
  { key: "patientAge", label: "Age" },
  { key: "patientGender", label: "Gender" },
  { key: "patientId", label: "Patient ID" },
  { key: "providerName", label: "Provider" },
  { key: "providerAddress", label: "Provider address" },
  { key: "diagnosis", label: "Diagnosis" },
  { key: "procedures", label: "Procedures" },
  { key: "admissionDate", label: "Admitted" },
  { key: "dischargeDate", label: "Discharged" },
  { key: "invoiceNumber", label: "Invoice no." },
  { key: "invoiceDate", label: "Invoice date" },
  { key: "totalAmount", label: "Total amount" },
  { key: "currency", label: "Currency" },
];

export const LOW_CONFIDENCE = 0.75;

export async function errorMessage(response: Response, fallback: string): Promise<string> {
  try {
    const body = await response.text();
    if (!body) {
      return `${fallback} (${response.status})`;
    }
    try {
      const parsed = JSON.parse(body);
      const detail = parsed.message ?? parsed.detail ?? parsed.error;
      if (typeof detail === "string" && detail.length > 0) {
        return detail;
      }
    } catch {
      return body.slice(0, 300);
    }
    return `${fallback} (${response.status})`;
  } catch {
    return `${fallback} (${response.status})`;
  }
}

export function isRunning(extraction: Extraction | null | undefined): boolean {
  return extraction?.status === "PENDING" || extraction?.status === "RUNNING";
}

export function fieldValue(extraction: Extraction, key: ExtractionFieldKey): string {
  if (key === "procedures") {
    return extraction.procedures.join(", ");
  }
  const value = extraction[key];
  return value == null ? "" : String(value);
}

export function confidenceOf(extraction: Extraction, key: ExtractionFieldKey): number | null {
  const value = extraction.confidence?.[key];
  return typeof value === "number" ? value : null;
}

export function formatAmount(value: string | null, currency: string | null): string {
  if (value == null) {
    return "—";
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return value;
  }
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currency && currency.length === 3 ? currency : "INR",
    maximumFractionDigits: 2,
  }).format(amount);
}
