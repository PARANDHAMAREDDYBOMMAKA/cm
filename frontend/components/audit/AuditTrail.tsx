import Link from "next/link";
import { ScrollText } from "lucide-react";
import { actionLabel, actionStyle, shortHash, type AuditEvent } from "@/lib/audit";

type Props = {
  events: AuditEvent[];
  showClaim?: boolean;
};

export default function AuditTrail({ events, showClaim = false }: Props) {
  if (events.length === 0) {
    return (
      <div className="mt-3 flex flex-col items-center justify-center rounded-xl border border-dashed border-border-strong bg-surface px-6 py-12 text-center">
        <span className="flex size-12 items-center justify-center rounded-xl bg-brand-soft text-brand">
          <ScrollText className="size-6" />
        </span>
        <h2 className="mt-4 text-base font-semibold">Nothing recorded yet</h2>
        <p className="mt-1 max-w-sm text-sm text-muted">
          Every action on a claim is sealed into the audit chain as it happens.
        </p>
      </div>
    );
  }

  return (
    <ul className="mt-3 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
      {events.map((event) => (
        <li key={event.id} className="px-5 py-3">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-[11px] tabular-nums text-subtle">#{event.seq}</span>
            <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${actionStyle(event.action)}`}>
              {actionLabel(event.action)}
            </span>
            {showClaim && event.claimId && event.claimReference ? (
              <Link
                href={`/dashboard/claims/${event.claimId}`}
                className="text-xs font-medium text-brand hover:underline"
              >
                {event.claimReference}
              </Link>
            ) : null}
            <span className="text-xs text-subtle">
              {event.actor} · {new Date(event.createdAt).toLocaleString()}
            </span>
          </div>
          <p className="mt-1 text-sm text-secondary">{event.summary}</p>
          {Object.keys(event.details ?? {}).length > 0 ? (
            <dl className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1">
              {Object.entries(event.details).map(([key, value]) => (
                <div key={key} className="flex gap-1.5 text-xs">
                  <dt className="text-subtle">{key}</dt>
                  <dd className="break-all text-secondary">{value}</dd>
                </div>
              ))}
            </dl>
          ) : null}
          <p className="mt-1.5 font-mono text-[11px] text-subtle">
            {shortHash(event.previousHash)} → {shortHash(event.hash)}
          </p>
        </li>
      ))}
    </ul>
  );
}
