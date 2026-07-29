import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, ExternalLink, FileText } from "lucide-react";
import { backendFetch, classifyFailure } from "@/lib/backend";
import LoadError from "@/components/dashboard/LoadError";
import { proxyUrl } from "@/lib/api";
import UploadPanel from "@/components/claims/UploadPanel";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import DeleteClaimButton from "@/components/claims/DeleteClaimButton";
import DeleteDocumentButton from "@/components/claims/DeleteDocumentButton";
import ExtractionPanel from "@/components/claims/ExtractionPanel";
import AutoRefresh from "@/components/claims/AutoRefresh";
import { isRunning, type Extraction } from "@/lib/extraction";
import RiskPanel from "@/components/claims/RiskPanel";
import type { Risk } from "@/lib/risk";

export const dynamic = "force-dynamic";

type DocumentItem = {
  id: string;
  filename: string;
  contentType: string | null;
  sizeBytes: number;
  createdAt: string;
  extraction: Extraction | null;
};

type ClaimDetail = {
  id: string;
  reference: string;
  claimantName: string | null;
  note: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
  documents: DocumentItem[];
  risk?: Risk | null;
};

const statusStyles: Record<string, string> = {
  RECEIVED: "bg-brand-soft text-brand",
  PROCESSING: "bg-warning-soft text-warning",
  EXTRACTED: "bg-brand-soft text-brand",
  FAILED: "bg-danger-soft text-danger",
  APPROVED: "bg-success-soft text-success",
  FLAGGED: "bg-danger-soft text-danger",
};

function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default async function ClaimDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const response = await backendFetch(`/api/claims/${id}`);

  if (response.status === 404) {
    notFound();
  }
  if (!response.ok) {
    return (
      <div className="mx-auto max-w-4xl">
        <LoadError kind={await classifyFailure(response)} what="this claim" />
      </div>
    );
  }

  const claim: ClaimDetail = await response.json();
  const processing = claim.documents.some((document) => isRunning(document.extraction));

  return (
    <div className="animate-in mx-auto max-w-4xl">
      <AutoRefresh active={processing} />
      <Link
        href="/dashboard/claims"
        className="inline-flex items-center gap-1.5 text-sm text-muted transition-colors hover:text-ink"
      >
        <ArrowLeft className="size-4" />
        Claims
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{claim.reference}</h1>
            <span
              className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusStyles[claim.status] ?? "bg-canvas text-muted"}`}
            >
              {claim.status}
            </span>
          </div>
          {claim.claimantName ? (
            <p className="mt-1 text-sm text-secondary">{claim.claimantName}</p>
          ) : null}
          <p className="mt-1 text-sm text-muted">Created {new Date(claim.createdAt).toLocaleString()}</p>
        </div>
        <div className="flex items-center gap-2">
          <ClaimFormDialog
            mode="edit"
            claimId={claim.id}
            initial={{
              reference: claim.reference,
              claimantName: claim.claimantName ?? "",
              note: claim.note ?? "",
              status: claim.status,
            }}
          />
          <DeleteClaimButton claimId={claim.id} />
        </div>
      </div>

      {claim.note ? (
        <div className="mt-4 rounded-xl border border-border bg-surface p-4 text-sm text-secondary">{claim.note}</div>
      ) : null}

      <RiskPanel risk={claim.risk} />

      <div className="mt-8">
        <h2 className="text-sm font-semibold text-secondary">Documents</h2>
        {claim.documents.length === 0 ? (
          <p className="mt-2 text-sm text-muted">No documents uploaded yet.</p>
        ) : (
          <ul className="mt-3 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
            {claim.documents.map((document) => {
              const contentUrl = proxyUrl(`/api/claims/${claim.id}/documents/${document.id}/content`);
              const isImage = (document.contentType ?? "").startsWith("image/");
              return (
                <li key={document.id}>
                  <div className="flex items-center gap-3 px-5 py-3">
                    {isImage ? (
                      <a href={contentUrl} target="_blank" rel="noopener noreferrer" className="shrink-0">
                        <img
                          src={contentUrl}
                          alt={document.filename}
                          className="size-10 rounded-lg border border-border object-cover"
                        />
                      </a>
                    ) : (
                      <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-canvas text-muted">
                        <FileText className="size-4" />
                      </span>
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{document.filename}</p>
                      <p className="text-xs text-subtle">
                        {document.contentType ?? "unknown"} · {formatBytes(document.sizeBytes)}
                      </p>
                    </div>
                    <a
                      href={contentUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink"
                    >
                      <ExternalLink className="size-3.5" />
                      View
                    </a>
                    <DeleteDocumentButton claimId={claim.id} documentId={document.id} />
                  </div>
                  <ExtractionPanel
                    claimId={claim.id}
                    documentId={document.id}
                    extraction={document.extraction}
                  />
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <div className="mt-6">
        <UploadPanel claimId={claim.id} />
      </div>
    </div>
  );
}
