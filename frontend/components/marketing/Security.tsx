import { Database, KeyRound, Lock, ScrollText, ShieldCheck } from "lucide-react";

const points = [
  {
    icon: KeyRound,
    title: "OIDC via Zitadel",
    description: "Sign-in is delegated to Zitadel over OpenID Connect — no passwords of ours to lose.",
  },
  {
    icon: ShieldCheck,
    title: "Tokens never reach the browser",
    description:
      "Access tokens are held by a server-side proxy. The browser never sees a credential it could leak.",
  },
  {
    icon: ScrollText,
    title: "Append-only audit log",
    description: "The audit trail is enforced append-only at the database layer, not just in application code.",
  },
  {
    icon: Database,
    title: "Synthetic data in v1",
    description: "This build runs on synthetic claims data only — no real patient health information.",
  },
  {
    icon: Lock,
    title: "Encrypted in transit",
    description: "All traffic between the browser, the application, and the backend runs over TLS.",
  },
];

export default function Security() {
  return (
    <section id="security" className="border-t border-border bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Built to be trusted, not just used</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Security and auditability are not an afterthought — they are in the request path.
          </p>
        </div>

        <div className="mt-16 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {points.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-2xl border border-border bg-canvas p-6">
              <div className="flex size-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
                <Icon className="size-5" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-ink">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-secondary">{description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
