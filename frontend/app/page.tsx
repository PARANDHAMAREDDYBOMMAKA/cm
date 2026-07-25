import Link from "next/link";
import { redirect } from "next/navigation";
import { auth, isAuthConfigured, signIn } from "@/lib/auth";

export default async function Home() {
  const session = await auth();
  if (session) {
    redirect("/dashboard");
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="w-full max-w-md rounded-2xl border border-zinc-200 bg-white p-10 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <div className="mb-8 flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-600 text-sm font-bold text-white">
            CG
          </span>
          <span className="text-lg font-semibold tracking-tight">ClaimGuard</span>
        </div>

        <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
          Authenticity-first claims intelligence. Verify every claim before it is paid.
        </p>

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
              className="w-full rounded-lg bg-emerald-600 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
            >
              Continue with Zitadel
            </button>
          </form>
        ) : (
          <div className="mt-8 space-y-4">
            <button
              type="button"
              disabled
              className="w-full cursor-not-allowed rounded-lg bg-zinc-200 py-2.5 text-sm font-medium text-zinc-400 dark:bg-zinc-800 dark:text-zinc-600"
            >
              Continue with Zitadel
            </button>
            <p className="text-xs text-amber-600 dark:text-amber-500">
              Auth provider not configured yet. Add your Zitadel credentials to enable sign in.
            </p>
          </div>
        )}

        <Link
          href="/dashboard"
          className="mt-6 block text-center text-sm text-zinc-500 underline-offset-4 hover:underline dark:text-zinc-400"
        >
          Preview the dashboard
        </Link>
      </div>
    </main>
  );
}
