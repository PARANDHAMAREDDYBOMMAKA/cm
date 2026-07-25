import { auth, isAuthConfigured, signIn, signOut } from "@/lib/auth";

export default async function Topbar() {
  const session = await auth();
  const user = session?.user;

  return (
    <header className="flex h-16 items-center justify-between border-b border-zinc-200 bg-white px-8 dark:border-zinc-800 dark:bg-zinc-900">
      <h1 className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Console</h1>

      {user ? (
        <div className="flex items-center gap-4">
          <span className="text-sm text-zinc-600 dark:text-zinc-300">
            {user.email ?? user.name}
          </span>
          <form
            action={async () => {
              "use server";
              await signOut({ redirectTo: "/" });
            }}
          >
            <button
              type="submit"
              className="rounded-lg border border-zinc-200 px-3 py-1.5 text-sm text-zinc-600 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800"
            >
              Sign out
            </button>
          </form>
        </div>
      ) : isAuthConfigured ? (
        <form
          action={async () => {
            "use server";
            await signIn("zitadel", { redirectTo: "/dashboard" });
          }}
        >
          <button
            type="submit"
            className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
          >
            Sign in
          </button>
        </form>
      ) : (
        <span className="text-xs text-amber-600 dark:text-amber-500">Guest preview</span>
      )}
    </header>
  );
}
