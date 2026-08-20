import { ShieldCheck } from "lucide-react";

export default function Logo({
  showWordmark = true,
  className = "",
}: {
  showWordmark?: boolean;
  className?: string;
}) {
  return (
    <span className={`flex items-center gap-2.5 ${className}`}>
      <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-brand text-white">
        <ShieldCheck className="size-4.5" />
      </span>
      {showWordmark ? (
        <span className="text-base font-semibold tracking-tight">ClaimGuard</span>
      ) : null}
    </span>
  );
}
