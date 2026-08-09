"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useRequireAuth, useAuth } from "@/lib/auth";
import { getSetupStatus } from "@/lib/api";
import Sidebar, { MobileTabBar } from "@/components/Sidebar";
import HeaderBar from "@/components/HeaderBar";

function titleFor(pathname: string): string {
  if (pathname.startsWith("/tasks")) return "Tasks";
  if (pathname.startsWith("/templates")) return "Templates";
  if (pathname.startsWith("/players")) return "Players";
  if (pathname.startsWith("/monitoring")) return "Monitoring";
  if (pathname.startsWith("/extensions")) return "Extensions";
  if (pathname.startsWith("/settings")) return "Settings";
  if (pathname.startsWith("/services/")) return decodeURIComponent(pathname.split("/")[2] ?? "");
  return "Services";
}

function parentFor(pathname: string): { label: string; href: string } | null {
  if (pathname.startsWith("/services/")) return { label: "Services", href: "/" };
  return null;
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const token = useRequireAuth();
  const { logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!token) return;
    getSetupStatus(token)
      .then(({ needsSetup }) => {
        if (needsSetup) router.replace("/setup");
      })
      .catch(() => {});
  }, [token, router]);

  if (!token) return null;

  return (
    <div className="flex h-full min-h-0">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col">
        <HeaderBar title={titleFor(pathname)} parent={parentFor(pathname)} onLogout={logout} />

        <main className="custom-scrollbar min-h-0 flex-1 overflow-y-auto px-4 pb-20 pt-5 sm:px-6 sm:pb-6 lg:px-8">
          <div key={pathname} className="page-in mx-auto w-full max-w-5xl">
            {children}
          </div>
        </main>
      </div>

      <MobileTabBar />
    </div>
  );
}
