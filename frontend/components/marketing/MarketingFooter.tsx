import Logo from "@/components/brand/Logo";

export default function MarketingFooter() {
  return (
    <footer className="bg-canvas">
      <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-6 py-10 sm:flex-row">
        <Logo />
        <p className="text-xs text-subtle">ClaimGuard · v0.1</p>
      </div>
    </footer>
  );
}
