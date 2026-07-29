import { AlertOctagon, ShieldCheck, ShieldQuestion } from "lucide-react";
import {
  BAND_STYLES,
  isFlagged,
  SEVERITY_STYLES,
  signalLabel,
  type Risk,
} from "@/lib/risk";

export default function RiskPanel({ risk }: { risk?: Risk | null }) {
  const flagged = risk ? isFlagged(risk.band) : false;

  if (!risk || risk.band === "UNASSESSED") {
    return (
      <div className="mt-6 flex items-center gap-3 rounded-xl border border-border bg-surface px-5 py-4 text-sm text-muted">
        <ShieldQuestion className="size-4 shrink-0" />
        Not yet checked for fraud. This runs automatically once a document is read.
      </div>
    );
  }

  return (
    <div
      className={`mt-6 overflow-hidden rounded-xl border ${
        flagged ? "border-danger" : "border-border"
      } bg-surface`}
    >
      <div className={`flex flex-wrap items-center justify-between gap-3 px-5 py-4 ${
        flagged ? "bg-danger-soft" : ""
      }`}>
        <div className="flex items-center gap-3">
          {flagged ? (
            <AlertOctagon className="size-5 shrink-0 text-danger" />
          ) : (
            <ShieldCheck className="size-5 shrink-0 text-success" />
          )}
          <div>
            <p className={`text-sm font-semibold ${flagged ? "text-danger" : "text-ink"}`}>
              {flagged ? "Flagged for review" : "No fraud signals of concern"}
            </p>
            <p className="mt-0.5 text-xs text-secondary">
              {risk.signalCount} signal{risk.signalCount === 1 ? "" : "s"} · risk score {risk.score}/100
            </p>
          </div>
        </div>
        <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${BAND_STYLES[risk.band]}`}>
          {risk.band}
        </span>
      </div>

      {risk.signals?.length ? (
        <ul className="divide-y divide-border border-t border-border">
          {risk.signals.map((signal) => (
            <li key={signal.id} className="px-5 py-3">
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${SEVERITY_STYLES[signal.severity]}`}
                >
                  {signal.severity}
                </span>
                <span className="text-sm font-medium text-ink">{signalLabel(signal.type)}</span>
                <span className="text-xs text-subtle">+{signal.weight}</span>
              </div>
              <p className="mt-1 text-sm text-secondary">{signal.message}</p>
              {Object.keys(signal.details ?? {}).length > 0 ? (
                <dl className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1">
                  {Object.entries(signal.details ?? {}).map(([key, value]) => (
                    <div key={key} className="flex gap-1.5 text-xs">
                      <dt className="text-subtle">{key}</dt>
                      <dd className="text-secondary">{value}</dd>
                    </div>
                  ))}
                </dl>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
