import type { Metadata } from "next";
import { auth } from "@/lib/auth";
import MarketingNav from "@/components/marketing/MarketingNav";
import FeaturesHero from "@/components/marketing/FeaturesHero";
import ExtractionSection from "@/components/marketing/ExtractionSection";
import DuplicateDetectionSection from "@/components/marketing/DuplicateDetectionSection";
import ForensicsSection from "@/components/marketing/ForensicsSection";
import ConsistencySection from "@/components/marketing/ConsistencySection";
import DecisionEngineSection from "@/components/marketing/DecisionEngineSection";
import AuditTrailSection from "@/components/marketing/AuditTrailSection";
import InteropConsoleSection from "@/components/marketing/InteropConsoleSection";
import Security from "@/components/marketing/Security";
import StatusSection from "@/components/marketing/StatusSection";
import CtaBand from "@/components/marketing/CtaBand";
import MarketingFooter from "@/components/marketing/MarketingFooter";

export const metadata: Metadata = {
  title: "Features · ClaimGuard",
  description:
    "A full walkthrough of ClaimGuard's pipeline — document extraction, duplicate detection, forensics, consistency checks, risk scoring, the audit trail, and NHCX interop.",
};

export default async function FeaturesPage() {
  const session = await auth();
  const ctaHref = session ? "/dashboard" : "/signin";
  const ctaLabel = session ? "Go to console" : "Sign in";

  return (
    <>
      <MarketingNav ctaHref={ctaHref} ctaLabel={ctaLabel} />
      <main>
        <FeaturesHero />
        <ExtractionSection />
        <DuplicateDetectionSection />
        <ForensicsSection />
        <ConsistencySection />
        <DecisionEngineSection />
        <AuditTrailSection />
        <InteropConsoleSection />
        <Security />
        <StatusSection />
        <CtaBand ctaHref={ctaHref} ctaLabel={ctaLabel} />
      </main>
      <MarketingFooter />
    </>
  );
}
