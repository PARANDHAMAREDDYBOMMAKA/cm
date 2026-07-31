import Link from "next/link";
import { ArrowRight } from "lucide-react";

export default function CtaBand({
  ctaHref,
  ctaLabel,
}: {
  ctaHref: string;
  ctaLabel: string;
}) {
  return (
    <section className="bg-brand py-20 sm:py-24">
      <div className="mx-auto max-w-3xl px-6 text-center">
        <h2 className="text-3xl font-semibold tracking-tight text-white sm:text-4xl">
          See a claim go from upload to decision.
        </h2>
        <p className="mt-4 text-base leading-relaxed text-white/75">
          Walk through the pipeline yourself — upload a document, watch the risk score form, and
          check the audit trail it leaves behind.
        </p>
        <div className="mt-8 flex justify-center">
          <Link
            href={ctaHref}
            className="group flex items-center gap-2 rounded-lg bg-white px-5 py-2.5 text-sm font-medium text-brand transition-colors hover:bg-white/90"
          >
            {ctaLabel}
            <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>
      </div>
    </section>
  );
}
