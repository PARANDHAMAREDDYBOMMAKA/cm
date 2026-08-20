"use client";

import { usePathname } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { activeItem } from "@/lib/nav";

export default function PageTitle() {
  const pathname = usePathname();
  const item = activeItem(pathname);

  if (!item) {
    return <span className="text-sm font-medium text-ink">Console</span>;
  }

  const isDetail = pathname !== item.href;

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-sm">
      <span className={isDetail ? "text-muted" : "font-medium text-ink"}>{item.label}</span>
      {isDetail ? (
        <>
          <ChevronRight className="size-3.5 text-subtle" aria-hidden />
          <span className="font-medium text-ink">Detail</span>
        </>
      ) : null}
    </nav>
  );
}
