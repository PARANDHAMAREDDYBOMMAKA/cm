"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import Logo from "@/components/brand/Logo";
import { NAV_ITEMS, isActive } from "@/lib/nav";

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r border-border bg-surface lg:flex">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Link href="/dashboard" className="rounded-lg">
          <Logo />
        </Link>
      </div>

      <nav className="flex-1 p-3" aria-label="Console">
        <p className="px-3 pb-1 pt-3 text-xs font-medium uppercase tracking-wider text-subtle">
          Workspace
        </p>
        <div className="mt-1 space-y-0.5">
          {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
            const active = isActive(pathname, href);
            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? "page" : undefined}
                className={
                  active
                    ? "flex items-center gap-3 rounded-lg bg-brand-soft px-3 py-2 text-sm font-medium text-brand"
                    : "flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-secondary transition-colors hover:bg-canvas hover:text-ink"
                }
              >
                <Icon className="size-4.5" />
                {label}
              </Link>
            );
          })}
        </div>
      </nav>

      <div className="border-t border-border px-5 py-4">
        <p className="text-xs text-subtle">ClaimGuard · v0.1</p>
      </div>
    </aside>
  );
}
