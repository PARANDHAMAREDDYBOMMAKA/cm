import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, ChevronDown, ExternalLink, FileText } from "lucide-react";
import { backendFetch, classifyFailure } from "@/lib/backend";
import LoadError from "@/components/dashboard/LoadError";
import { proxyUrl } from "@/lib/api";
import UploadPanel from "@/components/claims/UploadPanel";
import ClaimFormDialog from "@/components/claims/ClaimFormDialog";
import DeleteClaimButton from "@/components/claims/DeleteClaimButton";
import DeleteDocumentButton from "@/components/claims/DeleteDocumentButton";
import ExtractionPanel from "@/components/claims/ExtractionPanel";
import NhcxPanel from "@/components/claims/NhcxPanel";
import AutoRefresh from "@/components/claims/AutoRefresh";
import ClaimTabs from "@/components/claims/ClaimTabs";
import ConsolidatedFacts from "@/components/claims/ConsolidatedFacts";
import { formatAmount, isRunning, type Extraction } from "@/lib/extraction";
import RiskPanel from "@/components/claims/RiskPanel";
import DecisionPanel from "@/components/claims/DecisionPanel";
import AuditTrail from "@/components/audit/AuditTrail";
import type { Risk } from "@/lib/risk";
import type { Decision } from "@/lib/decision";
import type { AuditEvent } from "@/lib/audit";
import { formatBytes, statusStyle } from "@/lib/claim";

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
  decision?: Decision | null;
};

function ExtractionSummary({ extraction }: { extraction: Extraction | null }) {
  if (!extraction) {
    return <span className="shrink-0 text-xs text-subtle">Not read</span>;
  }
  if (isRunning(extraction)) {
    return <span className="shrink-0 text-xs text-secondary">Reading…</span>;
  }
  if (extraction.status === "FAILED") {
    return (
      <span className="shrink-0 rounded-full bg-danger-soft px-2 py-0.5 text-xs font-medium text-danger">
        Failed
      </span>
    );
  }
  if (extraction.status !== "COMPLETED") {
    return <span className="shrink-0 text-xs text-subtle">{extraction.status}</span>;
  }
  return (
    <span className="shrink-0 text-sm font-medium tabular-nums text-ink">
      {formatAmount(extraction.totalAmount, extraction.currency)}
    </span>
  );
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
  const auditResponse = await backendFetch(`/api/audit/claims/${id}`);
  const auditEvents: AuditEvent[] = auditResponse.ok ? await auditResponse.json() : [];
  const auditFailure = auditResponse.ok ? null : await classifyFailure(auditResponse);

  const overview = (
    <>
      <ConsolidatedFacts documents={claim.documents} />
      <DecisionPanel claimId={claim.id} decision={claim.decision} />
      <RiskPanel risk={claim.risk} />
      <div className="mt-6">
        <NhcxPanel claimId={claim.id} />
      </div>
    </>
  );

  const documents = (
    <>
      {claim.documents.length === 0 ? (
        <div className="mt-4 rounded-xl border border-dashed border-border-strong bg-surface px-6 py-10 text-center">
          <p className="text-sm font-medium text-ink">No documents yet</p>
          <p className="mt-1 text-sm text-muted">Upload a bill below and it will be read automatically.</p>
        </div>
      ) : (
        <ul className="mt-4 space-y-3">
          {claim.documents.map((document, index) => {
            const contentUrl = proxyUrl(`/api/claims/${claim.id}/documents/${document.id}/content`);
            const isImage = (document.contentType ?? "").startsWith("image/");
            const extraction = document.extraction;
            return (
              <li key={document.id} className="overflow-hidden rounded-xl border border-border bg-surface">
                <details open={claim.documents.length === 1 || index === 0} className="group">
                  <summary className="flex cursor-pointer list-none items-center gap-3 px-5 py-3 hover:bg-canvas">
                    {isImage ? (
                      <Image
                        src={contentUrl}
                        alt={document.filename}
                        width={40}
                        height={40}
                        unoptimized
                        className="size-10 shrink-0 rounded-lg border border-border object-cover"
                      />
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
                    <ExtractionSummary extraction={extraction} />
                    <ChevronDown className="size-4 shrink-0 text-subtle transition-transform group-open:rotate-180" />
                  </summary>

                  <div className="border-t border-border">
                    <div className="flex flex-wrap items-center justify-end gap-2 px-5 py-2.5">
                      <a
                        href={contentUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-secondary transition-colors hover:bg-canvas hover:text-ink"
                      >
                        <ExternalLink className="size-3.5" />
                        View
                      </a>
                      <DeleteDocumentButton
                        claimId={claim.id}
                        documentId={document.id}
                        filename={document.filename}
                      />
                    </div>
                    <ExtractionPanel
                      claimId={claim.id}
                      documentId={document.id}
                      extraction={document.extraction}
                    />
                  </div>
                </details>
              </li>
            );
          })}
        </ul>
      )}
      <div className="mt-6">
        <UploadPanel claimId={claim.id} />
      </div>
    </>
  );

  const audit = auditFailure ? (
    <LoadError kind={auditFailure} what="this claim's audit trail" />
  ) : (
    <AuditTrail events={auditEvents} />
  );

  return (
    <div className="animate-in mx-auto max-w-4xl">
      <AutoRefresh claimId={claim.id} active={processing} />
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
            <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusStyle(claim.status)}`}>
              {claim.status}
            </span>
          </div>
          {claim.claimantName ? <p className="mt-1 text-sm text-secondary">{claim.claimantName}</p> : null}
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
          <DeleteClaimButton claimId={claim.id} reference={claim.reference} />
        </div>
      </div>

      {claim.note ? (
        <div className="mt-4 rounded-xl border border-border bg-surface p-4 text-sm text-secondary">
          {claim.note}
        </div>
      ) : null}

      <ClaimTabs
        tabs={[
          { key: "overview", label: "Overview", content: overview },
          { key: "documents", label: "Documents", count: claim.documents.length, content: documents },
          { key: "audit", label: "Audit", count: auditEvents.length, content: audit },
        ]}
      />
    </div>
  );
}
