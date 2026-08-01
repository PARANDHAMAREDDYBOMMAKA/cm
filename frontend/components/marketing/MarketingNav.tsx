import Link from "next/link";
import { ArrowRight, ShieldCheck } from "lucide-react";

const links = [
  { href: "/#how", label: "How it works" },
  { href: "/features", label: "Features" },
  { href: "/#security", label: "Security" },
];

export default function MarketingNav({
  ctaHref,
  ctaLabel,
}: {
  ctaHref: string;
  ctaLabel: string;
}) {
  return (
    <header className="sticky top-0 z-40 border-b border-border bg-surface/80 backdrop-blur-sm">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <Link href="/" className="flex items-center gap-2.5">
          <div className="flex size-8 items-center justify-center rounded-lg bg-brand text-white">
            <ShieldCheck className="size-4.5" />
          </div>
          <span className="text-base font-semibold tracking-tight">ClaimGuard</span>
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-secondary transition-colors hover:text-ink"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <Link
          href={ctaHref}
          className="group flex items-center gap-1.5 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
        >
          {ctaLabel}
          <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
        </Link>
      </div>
    </header>
  );
}
