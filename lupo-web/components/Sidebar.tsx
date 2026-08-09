"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ComponentType, SVGProps } from "react";
import { IconActivity, IconBolt, IconCpu, IconGrid, IconLayers, IconSettings, IconUsers } from "./icons";
import {ActivityIcon, CpuIcon, HomeIcon, Layers2Icon, LayersIcon, SettingsIcon, UsersIcon} from "lucide-react";

type NavEntry = {
  href: string;
  label: string;
  icon: ComponentType<SVGProps<SVGSVGElement>>;
  match: (path: string) => boolean;
};

const NAV_ENTRIES: readonly NavEntry[] = [
  { href: "/", label: "Home", icon: HomeIcon, match: (p) => p === "/" || p.startsWith("/services") },
  { href: "/tasks", label: "Tasks", icon: ActivityIcon, match: (p) => p.startsWith("/tasks") },
  { href: "/templates", label: "Templates", icon: LayersIcon, match: (p) => p.startsWith("/templates") },
  { href: "/players", label: "Players", icon: UsersIcon, match: (p) => p.startsWith("/players") },
  { href: "/extensions", label: "Extensions", icon: Layers2Icon, match: (p) => p.startsWith("/extensions") },
  { href: "/monitoring", label: "Monitoring", icon: CpuIcon, match: (p) => p.startsWith("/monitoring") },
  { href: "/settings", label: "Settings", icon: SettingsIcon, match: (p) => p.startsWith("/settings") },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <nav className="hidden w-18 shrink-0 flex-col items-center border-r bg-white/5 border border-white/10 py-5 sm:flex">
      <div className="mb-6 flex h-9 w-9 items-center justify-center border border-white/20 transition-transform duration-300 hover:rotate-12 hover:scale-110">

      </div>

      <div className="flex flex-1 flex-col items-center gap-1.5">
        {NAV_ENTRIES.map(({ href, label, icon: Icon, match }) => {
          const isActive = match(pathname);
          return (
            <Link
              key={href}
              href={href}
              title={label}
              className={`group relative flex h-10 w-10 items-center justify-center transition-all duration-200 ease-out active:scale-90 ${
                isActive ? "scale-105 text-black" : "text-white/40 hover:bg-white/10 hover:text-white"
              }`}
            >
              {isActive && <span className="absolute inset-0 animate-[scaleIn_0.25s_ease-out_both] bg-white" />}
              <Icon className="relative z-10 h-4.25 w-4.25 transition-transform duration-200 group-hover:scale-110" />
              <span className="pointer-events-none absolute left-full ml-3 translate-x-1 whitespace-nowrap border border-white/15 bg-black px-2 py-1 font-mono text-[10px] uppercase tracking-wider text-white opacity-0 transition-all duration-200 group-hover:translate-x-0 group-hover:opacity-100">
                {label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}

export function MobileTabBar() {
  const pathname = usePathname();

  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 flex items-center justify-around border-t border-white/10 bg-black px-2 py-2 sm:hidden">
      {NAV_ENTRIES.map(({ href, label, icon: Icon, match }) => {
        const isActive = match(pathname);
        return (
          <Link
            key={href}
            href={href}
            className={`flex flex-1 flex-col items-center gap-1 py-1.5 transition-all duration-150 active:scale-90 ${
              isActive ? "text-white" : "text-white/40"
            }`}
          >
            <Icon className={`h-5 w-5 transition-transform duration-200 ${isActive ? "scale-110" : ""}`} />
            <span className="font-mono text-[9px] uppercase tracking-wider">{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
