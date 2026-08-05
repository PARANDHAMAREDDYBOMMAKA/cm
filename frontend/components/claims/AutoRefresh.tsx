"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { proxyUrl } from "@/lib/api";

type Props = {
  claimId: string;
  active: boolean;
  intervalMs?: number;
  maxPolls?: number;
};

type Status = {
  status: string;
  updatedAt: string;
  documentCount: number;
  processing: boolean;
};

export default function AutoRefresh({ claimId, active, intervalMs = 4000, maxPolls = 75 }: Props) {
  const router = useRouter();
  const polls = useRef(0);
  const lastSignature = useRef<string | null>(null);

  useEffect(() => {
    if (!active) {
      polls.current = 0;
      lastSignature.current = null;
      return;
    }

    let cancelled = false;

    const tick = async () => {
      if (cancelled) {
        return;
      }
      polls.current += 1;
      if (polls.current > maxPolls) {
        window.clearInterval(timer);
        return;
      }
      try {
        const response = await fetch(proxyUrl(`/api/claims/${claimId}/status`), {
          cache: "no-store",
        });
        if (!response.ok || cancelled) {
          return;
        }
        const status: Status = await response.json();
        const signature = `${status.status}|${status.updatedAt}|${status.documentCount}|${status.processing}`;
        if (lastSignature.current === null) {
          lastSignature.current = signature;
          return;
        }
        if (signature !== lastSignature.current) {
          lastSignature.current = signature;
          router.refresh();
        }
        if (!status.processing) {
          window.clearInterval(timer);
          router.refresh();
        }
      } catch {
        return;
      }
    };

    const timer = window.setInterval(tick, intervalMs);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [active, claimId, intervalMs, maxPolls, router]);

  return null;
}
