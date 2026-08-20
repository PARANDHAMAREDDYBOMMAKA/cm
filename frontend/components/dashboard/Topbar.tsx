import { LogOut } from "lucide-react";
import MobileNav from "@/components/dashboard/MobileNav";
import PageTitle from "@/components/dashboard/PageTitle";
import { auth, isAuthConfigured, signIn, signOut } from "@/lib/auth";

function initials(value: string): string {
  const base = value.replace(/@.*/, "");
  const parts = base.split(/[.\s_-]+/).filter(Boolean);
  const letters = parts.slice(0, 2).map((part) => part[0]?.toUpperCase() ?? "");
  return letters.join("") || value[0]?.toUpperCase() || "U";
}

export default async function Topbar() {
  const session = await auth();
  const user = session?.user;
  const label = user?.email ?? user?.name ?? "";

  return (
    <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b border-border bg-surface px-4 sm:px-6">
      <div className="flex min-w-0 items-center gap-2">
        <MobileNav />
        <PageTitle />
      </div>

      {user ? (
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5">
            <span className="flex size-8 items-center justify-center rounded-full bg-brand-soft text-xs font-semibold text-brand">
              {initials(label)}
            </span>
            <span className="hidden max-w-[16ch] truncate text-sm text-secondary md:inline">{label}</span>
          </div>
          <form
            action={async () => {
              "use server";
              await signOut({ redirectTo: "/" });
            }}
          >
            <button
              type="submit"
              className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm text-secondary transition-colors hover:bg-canvas hover:text-ink"
            >
              <LogOut className="size-4" />
              <span className="hidden sm:inline">Sign out</span>
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
            className="rounded-lg bg-brand px-3.5 py-1.5 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
          >
            Sign in
          </button>
        </form>
      ) : (
        <span className="rounded-full bg-warning-soft px-2.5 py-1 text-xs font-medium text-warning">
          Guest preview
        </span>
      )}
    </header>
  );
}
