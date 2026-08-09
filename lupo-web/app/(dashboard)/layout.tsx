"use client";

import Link from "next/link";
import { useRequireAuth, useAuth } from "@/lib/auth";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const token = useRequireAuth();
  const { logout } = useAuth();

  if (!token) return null;

  return (
    <div className="flex min-h-full flex-col">
      <nav className="flex items-center gap-6 border-b border-neutral-800 px-6 py-3">
        <span className="font-semibold">lupo cloud</span>
        <Link href="/" className="text-sm text-neutral-300 hover:text-white">
          Services
        </Link>
        <Link href="/tasks" className="text-sm text-neutral-300 hover:text-white">
          Tasks
        </Link>
        <Link href="/templates" className="text-sm text-neutral-300 hover:text-white">
          Templates
        </Link>
        <button
          onClick={logout}
          className="ml-auto text-sm text-neutral-400 hover:text-white cursor-pointer"
        >
          Log out
        </button>
      </nav>
      <main className="flex-1 p-6">{children}</main>
    </div>
  );
}
