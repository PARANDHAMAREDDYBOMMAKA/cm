import { CircleCheck, CircleSlash, ShieldAlert, ShieldCheck, SlidersHorizontal } from "lucide-react";
import { auth } from "@/lib/auth";
import { backendFetch, classifyFailure } from "@/lib/backend";
import LoadError from "@/components/dashboard/LoadError";
import type { Settings } from "@/lib/settings";

export const dynamic = "force-dynamic";

function initials(value: string): string {
  const base = value.replace(/@.*/, "");
  const parts = base.split(/[.\s_-]+/).filter(Boolean);
  const letters = parts.slice(0, 2).map((part) => part[0]?.toUpperCase() ?? "");
  return letters.join("") || value[0]?.toUpperCase() || "U";
}

export default async function SettingsPage() {
  const [session, response] = await Promise.all([auth(), backendFetch("/api/settings")]);
  const settings: Settings | null = response.ok ? await response.json() : null;
  const failure = response.ok ? null : await classifyFailure(response);
  const user = session?.user;
  const signedIn = Boolean(user);
  const primaryLabel = user?.name ?? user?.email ?? "Signed in";
  const secondaryLabel = user?.email ?? user?.name ?? "";

  return (
    <div className="animate-in mx-auto max-w-4xl">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="mt-1 text-sm text-muted">
          What this deployment has wired up, and the thresholds it decides by.
        </p>
      </div>

      {failure ? (
        <LoadError kind={failure} what="settings" />
      ) : (
        <>
          <div className="mt-6 rounded-xl border border-border bg-surface p-5">
            <h2 className="text-sm font-semibold text-secondary">Account</h2>
            <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="flex size-9 items-center justify-center rounded-full bg-brand-soft text-xs font-semibold text-brand">
                  {initials(primaryLabel)}
                </span>
                <div>
                  <p className="text-sm font-medium text-ink">{signedIn ? primaryLabel : "Not signed in"}</p>
                  <p className="text-xs text-subtle">
                    {signedIn ? secondaryLabel : "Sign in to act on claims."}
                  </p>
                </div>
              </div>
              <span
                className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                  signedIn ? "bg-success-soft text-success" : "bg-warning-soft text-warning"
                }`}
              >
                {signedIn ? "Signed in" : "Guest preview"}
              </span>
            </div>
          </div>

          <div className="mt-6">
            <h2 className="text-sm font-semibold text-secondary">Capabilities</h2>
            <ul className="mt-3 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
              {settings?.capabilities.map((capability) => (
                <li key={capability.key} className="flex flex-wrap items-start gap-3 px-5 py-4">
                  {capability.configured ? (
                    <CircleCheck className="mt-0.5 size-4 shrink-0 text-success" />
                  ) : (
                    <CircleSlash className="mt-0.5 size-4 shrink-0 text-subtle" />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">{capability.name}</p>
                    <p className="mt-0.5 break-words text-xs text-secondary">{capability.detail}</p>
                  </div>
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${
                      capability.configured ? "bg-success-soft text-success" : "bg-canvas text-muted"
                    }`}
                  >
                    {capability.configured ? "Configured" : "Not configured"}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-6">
            <div className="flex items-center gap-2">
              <SlidersHorizontal className="size-4 text-brand" />
              <h2 className="text-sm font-semibold text-secondary">Decision thresholds</h2>
            </div>
            <ul className="mt-3 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
              {settings?.thresholds.map((threshold) => (
                <li key={threshold.name} className="flex flex-wrap items-start gap-3 px-5 py-4">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">{threshold.name}</p>
                    <p className="mt-0.5 text-xs text-secondary">{threshold.description}</p>
                  </div>
                  <span className="rounded-lg bg-canvas px-2.5 py-1 text-xs font-medium tabular-nums text-ink">
                    {threshold.value}
                  </span>
                </li>
              ))}
            </ul>
            <p className="mt-2 text-xs text-subtle">
              These are read from the backend environment. Change them there and restart to take effect.
            </p>
          </div>

          <div
            className={`mt-6 flex flex-wrap items-center gap-3 rounded-xl border px-5 py-4 ${
              settings?.auditIntact ? "border-border bg-surface" : "border-danger bg-danger-soft"
            }`}
          >
            {settings?.auditIntact ? (
              <ShieldCheck className="size-5 shrink-0 text-success" />
            ) : (
              <ShieldAlert className="size-5 shrink-0 text-danger" />
            )}
            <div className="min-w-0 flex-1">
              <p className={`text-sm font-semibold ${settings?.auditIntact ? "text-ink" : "text-danger"}`}>
                Audit chain {settings?.auditIntact ? "intact" : "broken"}
              </p>
              <p className="mt-0.5 text-xs text-secondary">{settings?.auditMessage}</p>
            </div>
            <span className="text-xs tabular-nums text-subtle">{settings?.auditEvents ?? 0} entries</span>
          </div>
        </>
      )}
    </div>
  );
}
