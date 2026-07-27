"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  FileText,
  LayoutDashboard,
  ScrollText,
  Settings,
  ShieldAlert,
  ShieldCheck,
  type LucideIcon,
} from "lucide-react";

type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  enabled: boolean;
};

const items: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, enabled: true },
  { href: "/dashboard/claims", label: "Claims", icon: FileText, enabled: false },
  { href: "/dashboard/review", label: "Review Queue", icon: ShieldAlert, enabled: false },
  { href: "/dashboard/audit", label: "Audit Trail", icon: ScrollText, enabled: false },
  { href: "/dashboard/settings", label: "Settings", icon: Settings, enabled: false },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-border bg-surface">
      <div className="flex h-16 items-center gap-2.5 border-b border-border px-5">
        <div className="flex size-8 items-center justify-center rounded-lg bg-brand text-white">
          <ShieldCheck className="size-4.5" />
        </div>
        <span className="text-base font-semibold tracking-tight">ClaimGuard</span>
      </div>

      <nav className="flex-1 p-3">
        <p className="px-3 pb-1 pt-3 text-xs font-medium uppercase tracking-wider text-subtle">
          Workspace
        </p>
        <div className="mt-1 space-y-0.5">
          {items.map(({ href, label, icon: Icon, enabled }) => {
            if (!enabled) {
              return (
                <div
                  key={href}
                  className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2 text-sm text-subtle"
                >
                  <Icon className="size-4.5" />
                  <span className="flex-1">{label}</span>
                  <span className="rounded-full bg-canvas px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-subtle">
                    Soon
                  </span>
                </div>
              );
            }

            const active = pathname === href;
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
