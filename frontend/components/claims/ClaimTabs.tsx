"use client";

import { useState, type ReactNode } from "react";

type Tab = {
  key: string;
  label: string;
  count?: number;
  content: ReactNode;
};

export default function ClaimTabs({ tabs }: { tabs: Tab[] }) {
  const [active, setActive] = useState(tabs[0]?.key ?? "");
  const current = tabs.find((tab) => tab.key === active) ?? tabs[0];

  return (
    <div className="mt-6">
      <div role="tablist" className="flex flex-wrap gap-1 border-b border-border">
        {tabs.map((tab) => {
          const selected = tab.key === current?.key;
          return (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={selected}
              onClick={() => setActive(tab.key)}
              className={
                selected
                  ? "-mb-px flex shrink-0 items-center gap-1.5 border-b-2 border-brand px-4 py-2.5 text-sm font-medium text-brand"
                  : "-mb-px flex shrink-0 items-center gap-1.5 border-b-2 border-transparent px-4 py-2.5 text-sm font-medium text-muted transition-colors hover:text-ink"
              }
            >
              {tab.label}
              {typeof tab.count === "number" ? (
                <span className="rounded-full bg-canvas px-1.5 py-0.5 text-[11px] tabular-nums text-subtle">
                  {tab.count}
                </span>
              ) : null}
            </button>
          );
        })}
      </div>
      <div role="tabpanel">{current?.content}</div>
    </div>
  );
}
