import { backendFetch, classifyFailure } from "@/lib/backend";
import LoadError from "@/components/dashboard/LoadError";
import AuditTrail from "@/components/audit/AuditTrail";
import SealBadge from "@/components/audit/SealBadge";
import type { AuditEvent, AuditVerification } from "@/lib/audit";

export const dynamic = "force-dynamic";

export default async function AuditPage() {
  const [eventsResponse, verifyResponse] = await Promise.all([
    backendFetch("/api/audit?limit=200"),
    backendFetch("/api/audit/verify"),
  ]);

  const events: AuditEvent[] = eventsResponse.ok ? await eventsResponse.json() : [];
  const verification: AuditVerification | null = verifyResponse.ok ? await verifyResponse.json() : null;
  const failure = eventsResponse.ok ? null : await classifyFailure(eventsResponse);

  return (
    <div className="animate-in mx-auto max-w-5xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Audit trail</h1>
          <p className="mt-1 text-sm text-muted">
            Every action, hash-chained in order. Changing one entry breaks every entry after it.
          </p>
        </div>
      </div>

      {failure ? (
        <LoadError kind={failure} what="the audit trail" />
      ) : (
        <>
          <div className="mt-6">
            <SealBadge verification={verification} />
          </div>
          <AuditTrail events={events} showClaim />
        </>
      )}
    </div>
  );
}
