"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { IconChevronDown } from "./icons";
import { LiveDot } from "./ui";

export default function HeaderBar({
  title,
  parent,
  onLogout,
}: {
  title: string;
  parent?: { label: string; href: string } | null;
  onLogout: () => void;
}) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    function onClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    }
    window.addEventListener("mousedown", onClickOutside);
    return () => window.removeEventListener("mousedown", onClickOutside);
  }, [menuOpen]);

  return (
    <header className="flex h-16 shrink-0 select-none items-center gap-4 border-b bg-white/5 border border-white/10 px-5">
      <div className="min-w-0 flex-1">
        <h2
          key={title}
          className="animate-[fadeInUp_0.3s_ease-out_both] flex min-w-0 items-center gap-2 truncate font-minecraft text-3xl tracking-wide text-white"
        >
          {parent && (
            <>
              <Link href={parent.href} className="shrink-0 text-white/40 transition-colors hover:text-white">
                {parent.label}
              </Link>
              <span className="shrink-0 text-white/25">/</span>
            </>
          )}
          <span className="truncate">{title}</span>
        </h2>
      </div>

      <div className="flex items-center gap-2 border border-white/15 px-3 py-2 transition-colors duration-200 hover:border-white/25">
        <LiveDot color="bg-white" />
        <span className="hidden font-mono text-[11px] text-white/50 sm:inline">Verbunden</span>
      </div>

      <div ref={menuRef} className="relative">
        <button
          onClick={() => setMenuOpen((v) => !v)}
          className="flex items-center gap-2 border border-white/15 py-1.5 pl-1.5 pr-2.5 transition-all duration-150 hover:bg-white/10 active:scale-95"
        >
          <span className="flex h-6 w-6 items-center justify-center border border-white/20 text-[10px] font-semibold text-white">
            A
          </span>
          <span className="text-xs font-medium text-white">admin</span>
          <IconChevronDown
            className={`h-3.5 w-3.5 text-white/40 transition-transform duration-200 ${menuOpen ? "rotate-180" : ""}`}
          />
        </button>

        {menuOpen && (
          <div className="absolute right-0 top-[calc(100%+6px)] z-40 w-48 origin-top-right animate-[scaleIn_0.15s_ease-out_both] border border-white/15 bg-black">
            <div className="border-b border-white/10 px-3 py-2.5">
              <div className="text-xs font-medium text-white">admin</div>
              <div className="font-mono text-[10px] text-white/35">lupo.cloud</div>
            </div>
            <button
              onClick={() => {
                setMenuOpen(false);
                onLogout();
              }}
              className="w-full px-3 py-2.5 text-left font-mono text-xs text-white/70 transition-colors hover:bg-white/10 hover:text-white"
            >
              Abmelden
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
