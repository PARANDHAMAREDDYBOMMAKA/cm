import { redirect } from "next/navigation";
import Sidebar from "@/components/dashboard/Sidebar";
import Topbar from "@/components/dashboard/Topbar";
import FingerprintBoot from "@/components/dashboard/FingerprintBoot";
import SupportWidget from "@/components/dashboard/SupportWidget";
import Analytics from "@/components/dashboard/Analytics";
import ToastProvider from "@/components/ui/Toast";
import { auth, isAuthConfigured } from "@/lib/auth";

export default async function DashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const session = await auth();
  if (isAuthConfigured) {
    if (session?.error) {
      redirect("/signin?expired=1");
    }
    if (!session?.user) {
      redirect("/signin");
    }
  }

  return (
    <ToastProvider>
      <div className="flex min-h-screen bg-canvas">
        <FingerprintBoot />
        <SupportWidget />
        <Analytics />
        <Sidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar />
          <main className="flex-1 px-4 py-6 sm:px-6 sm:py-8">{children}</main>
        </div>
      </div>
    </ToastProvider>
  );
}
