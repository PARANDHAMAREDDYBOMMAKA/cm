"use client";

import { useEffect, useState } from "react";
import { apiUrl } from "@/lib/api";

type Status = "checking" | "online" | "offline";

const presentation: Record<Status, { label: string; dot: string; text: string }> = {
  checking: { label: "Checking", dot: "bg-subtle", text: "text-muted" },
  online: { label: "Backend online", dot: "bg-success", text: "text-secondary" },
  offline: { label: "Backend offline", dot: "bg-danger", text: "text-danger" },
};

export default function BackendStatus() {
  const [status, setStatus] = useState<Status>("checking");

  useEffect(() => {
    let active = true;

    const check = async () => {
      try {
        const response = await fetch(apiUrl("/api/ping"), { cache: "no-store" });
        if (active) {
          setStatus(response.ok ? "online" : "offline");
        }
      } catch {
        if (active) {
          setStatus("offline");
        }
      }
    };

    check();
    const interval = setInterval(check, 15000);

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const { label, dot, text } = presentation[status];

  return (
    <div
      className={`inline-flex items-center gap-2 rounded-full border border-border bg-surface px-3 py-1.5 text-xs font-medium ${text}`}
    >
      <span className="relative flex size-2">
        {status === "online" && (
          <span className={`absolute inline-flex size-full animate-ping rounded-full ${dot} opacity-60`} />
        )}
        <span className={`relative inline-flex size-2 rounded-full ${dot}`} />
      </span>
      {label}
    </div>
  );
}
