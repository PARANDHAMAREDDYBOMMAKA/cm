import Link from "next/link";
import { redirect } from "next/navigation";
import { ArrowRight, FileCheck2, ScanLine, ShieldCheck } from "lucide-react";
import { auth, isAuthConfigured, signIn } from "@/lib/auth";

const highlights = [
  { icon: ScanLine, text: "Extract structured data from messy claim documents" },
  { icon: ShieldCheck, text: "Detect tampered, duplicate, and AI-generated claims" },
  { icon: FileCheck2, text: "Auto-approve clean claims, flag the rest for review" },
];

export default async function SignInPage({
  searchParams,
}: {
  searchParams: Promise<{ expired?: string }>;
}) {
  const [session, params] = await Promise.all([auth(), searchParams]);
  if (session && !session.error) {
    redirect("/dashboard");
  }
  const expired = params.expired === "1";

  return (
    <main className="grid min-h-screen lg:grid-cols-2">
      <section className="relative hidden flex-col justify-between bg-brand p-12 text-white lg:flex">
        <div className="flex items-center gap-2.5">
          <div className="flex size-9 items-center justify-center rounded-xl bg-white/15">
            <ShieldCheck className="size-5" />
          </div>
          <span className="text-lg font-semibold tracking-tight">ClaimGuard</span>
        </div>

        <div className="max-w-md">
          <h1 className="text-3xl font-semibold leading-tight tracking-tight">
            Verify every claim before it is paid.
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-white/70">
            Authenticity-first claims intelligence — read the documents, catch the fakes, approve the
            clean ones.
          </p>
          <ul className="mt-10 space-y-4">
            {highlights.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3">
                <Icon className="mt-0.5 size-5 shrink-0 text-white/80" />
                <span className="text-sm text-white/85">{text}</span>
              </li>
            ))}
          </ul>
        </div>

        <p className="text-xs text-white/50">Secured with OIDC · Auditable by design</p>
      </section>

      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-10 flex items-center gap-2.5 lg:hidden">
            <div className="flex size-9 items-center justify-center rounded-xl bg-brand text-white">
              <ShieldCheck className="size-5" />
            </div>
            <span className="text-lg font-semibold tracking-tight">ClaimGuard</span>
          </div>

          <h2 className="text-2xl font-semibold tracking-tight">Sign in</h2>
          <p className="mt-2 text-sm text-muted">Continue to your claims console.</p>

          {expired ? (
            <p className="mt-4 rounded-lg border border-warning bg-warning-soft px-3 py-2 text-sm text-warning">
              Your session expired and could not be renewed. Sign in again to continue.
            </p>
          ) : null}

          {isAuthConfigured ? (
            <form
              className="mt-8"
              action={async () => {
                "use server";
                await signIn("zitadel", { redirectTo: "/dashboard" });
              }}
            >
              <button
                type="submit"
                className="group flex w-full items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
              >
                Continue with Zitadel
                <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
              </button>
            </form>
          ) : (
            <div className="mt-8 space-y-3">
              <button
                type="button"
                disabled
                className="w-full cursor-not-allowed rounded-lg bg-border px-4 py-2.5 text-sm font-medium text-muted"
              >
                Continue with Zitadel
              </button>
              <p className="text-xs text-warning">Auth provider not configured yet.</p>
            </div>
          )}

          <Link
            href="/dashboard"
            className="mt-6 block text-center text-sm text-muted underline-offset-4 transition-colors hover:text-ink"
          >
            Preview the dashboard
          </Link>
        </div>
      </section>
    </main>
  );
}
