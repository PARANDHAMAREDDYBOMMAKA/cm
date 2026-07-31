import { ShieldCheck } from "lucide-react";

export default function MarketingFooter() {
  return (
    <footer className="bg-canvas">
      <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-6 py-10 sm:flex-row">
        <div className="flex items-center gap-2.5">
          <div className="flex size-8 items-center justify-center rounded-lg bg-brand text-white">
            <ShieldCheck className="size-4.5" />
          </div>
          <span className="text-sm font-semibold tracking-tight text-ink">ClaimGuard</span>
        </div>
        <p className="text-xs text-subtle">ClaimGuard · v0.1</p>
      </div>
    </footer>
  );
}
