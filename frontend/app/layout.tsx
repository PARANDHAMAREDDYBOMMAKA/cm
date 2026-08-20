import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const description =
  "ClaimGuard reads claim documents, catches duplicates and tampering, and auto-approves the clean ones with a tamper-evident audit trail.";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.AUTH_URL ?? "https://claimguard-pi.vercel.app"),
  title: {
    default: "ClaimGuard",
    template: "%s · ClaimGuard",
  },
  description,
  applicationName: "ClaimGuard",
  openGraph: {
    type: "website",
    siteName: "ClaimGuard",
    title: "ClaimGuard",
    description,
  },
  twitter: {
    card: "summary_large_image",
    title: "ClaimGuard",
    description,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full bg-canvas font-sans text-ink">{children}</body>
    </html>
  );
}
