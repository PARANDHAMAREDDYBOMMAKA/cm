"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, X } from "lucide-react";
import Logo from "@/components/brand/Logo";
import { NAV_ITEMS, isActive } from "@/lib/nav";

export default function MobileNav() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open]);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Open navigation"
        aria-expanded={open}
        className="-ml-1 flex size-9 items-center justify-center rounded-lg text-secondary transition-colors hover:bg-canvas hover:text-ink lg:hidden"
      >
        <Menu className="size-5" />
      </button>

      {open ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div
            className="absolute inset-0 bg-ink/40"
            onClick={() => setOpen(false)}
            aria-hidden
          />
          <div className="absolute inset-y-0 left-0 flex w-72 max-w-[85vw] flex-col bg-surface shadow-xl">
            <div className="flex h-16 shrink-0 items-center justify-between border-b border-border px-5">
              <Logo />
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Close navigation"
                className="flex size-9 items-center justify-center rounded-lg text-secondary transition-colors hover:bg-canvas hover:text-ink"
              >
                <X className="size-5" />
              </button>
            </div>

            <nav className="flex-1 overflow-y-auto p-3" aria-label="Console">
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
                      onClick={() => setOpen(false)}
                      aria-current={active ? "page" : undefined}
                      className={
                        active
                          ? "flex items-center gap-3 rounded-lg bg-brand-soft px-3 py-2.5 text-sm font-medium text-brand"
                          : "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-secondary transition-colors hover:bg-canvas hover:text-ink"
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
          </div>
        </div>
      ) : null}
    </>
  );
}
