"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";

type Props = {
  active: boolean;
  intervalMs?: number;
  maxPolls?: number;
};

export default function AutoRefresh({ active, intervalMs = 4000, maxPolls = 75 }: Props) {
  const router = useRouter();
  const polls = useRef(0);

  useEffect(() => {
    if (!active) {
      polls.current = 0;
      return;
    }
    const timer = setInterval(() => {
      polls.current += 1;
      if (polls.current > maxPolls) {
        clearInterval(timer);
        return;
      }
      router.refresh();
    }, intervalMs);
    return () => clearInterval(timer);
  }, [active, intervalMs, maxPolls, router]);

  return null;
}
