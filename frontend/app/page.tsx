import { auth } from "@/lib/auth";
import MarketingNav from "@/components/marketing/MarketingNav";
import Hero from "@/components/marketing/Hero";
import HowItWorks from "@/components/marketing/HowItWorks";
import Features from "@/components/marketing/Features";
import Security from "@/components/marketing/Security";
import CtaBand from "@/components/marketing/CtaBand";
import MarketingFooter from "@/components/marketing/MarketingFooter";

export default async function Home() {
  const session = await auth();
  const ctaHref = session ? "/dashboard" : "/signin";
  const ctaLabel = session ? "Go to console" : "Sign in";

  return (
    <>
      <MarketingNav ctaHref={ctaHref} ctaLabel={ctaLabel} />
      <main>
        <Hero ctaHref={ctaHref} ctaLabel={ctaLabel} />
        <HowItWorks />
        <Features />
        <Security />
        <CtaBand ctaHref={ctaHref} ctaLabel={ctaLabel} />
      </main>
      <MarketingFooter />
    </>
  );
}
