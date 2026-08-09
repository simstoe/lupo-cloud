"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth, useRequireAuth } from "@/lib/auth";
import { completeSetup, createTask, startTask } from "@/lib/api";
import type { ServiceTask } from "@/lib/types";
import { IconServer, IconGrid, IconCheck } from "@/components/icons";

function proxyTask(name: string, port: number): ServiceTask {
  return {
    name,
    type: "VELOCITY",
    templateName: null,
    minMemoryMB: 512,
    maxMemoryMB: 1024,
    cpuCoreLimit: null,
    startPort: port,
    minOnlineCount: 1,
    maxOnlineCount: 1,
    staticService: true,
    autostart: true,
    group: null,
    proxyGroups: null,
  };
}

function lobbyTask(name: string, port: number): ServiceTask {
  return {
    name,
    type: "PAPER",
    templateName: null,
    minMemoryMB: 1024,
    maxMemoryMB: 2048,
    cpuCoreLimit: null,
    startPort: port,
    minOnlineCount: 1,
    maxOnlineCount: 1,
    staticService: true,
    autostart: true,
    group: null,
    proxyGroups: null,
  };
}

export default function SetupPage() {
  const token = useRequireAuth();
  const { logout } = useAuth();
  const router = useRouter();

  const [proxyName, setProxyName] = useState("proxy");
  const [proxyPort, setProxyPort] = useState(25565);
  const [lobbyName, setLobbyName] = useState("lobby");
  const [lobbyPort, setLobbyPort] = useState(25580);
  const [startNow, setStartNow] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!token) return null;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const proxy = proxyTask(proxyName, proxyPort);
      const lobby = lobbyTask(lobbyName, lobbyPort);
      await createTask(token!, lobby);
      await createTask(token!, proxy);

      if (startNow) {
        await startTask(token!, lobbyName);
        await startTask(token!, proxyName);
      }

      await completeSetup(token!);
      router.replace("/");
    } catch {
      setError("Einrichtung fehlgeschlagen. Bitte Eingaben prüfen und erneut versuchen.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSkip() {
    setLoading(true);
    try {
      await completeSetup(token!);
      router.replace("/");
    } catch {
      setError("Überspringen fehlgeschlagen.");
      setLoading(false);
    }
  }

  return (
    <div className="bg-grid flex h-full items-center justify-center p-6">
      <div className="w-full max-w-lg animate-[fadeInUp_0.5s_ease-out_both] border border-white/15 bg-[#0a0a0a] p-8">
        <div className="flex items-center justify-between">
          <div>
            <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Erste Einrichtung</span>
            <h1 className="font-minecraft text-3xl uppercase tracking-wide text-white">Cloud einrichten</h1>
          </div>
          <button
            type="button"
            onClick={logout}
            className="font-mono text-[10px] uppercase tracking-wider text-white/30 hover:text-white/60"
          >
            Abmelden
          </button>
        </div>

        <p className="mt-3 font-mono text-[11px] leading-relaxed text-white/40">
          Lege einen Standard-Proxy und eine Lobby an, um sofort loszulegen. Du kannst später jederzeit weitere Tasks
          über das Dashboard anlegen.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-5">
          <div className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-wider text-white/50">
            <IconGrid className="h-3.5 w-3.5" /> Proxy
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Name</span>
              <input
                value={proxyName}
                onChange={(e) => setProxyName(e.target.value)}
                disabled={loading}
                className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Port</span>
              <input
                type="number"
                value={proxyPort}
                onChange={(e) => setProxyPort(Number(e.target.value))}
                disabled={loading}
                className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
              />
            </label>
          </div>

          <div className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-wider text-white/50">
            <IconServer className="h-3.5 w-3.5" /> Lobby
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Name</span>
              <input
                value={lobbyName}
                onChange={(e) => setLobbyName(e.target.value)}
                disabled={loading}
                className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Port</span>
              <input
                type="number"
                value={lobbyPort}
                onChange={(e) => setLobbyPort(Number(e.target.value))}
                disabled={loading}
                className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
              />
            </label>
          </div>

          <label className="flex items-center gap-2 font-mono text-[11px] text-white/50">
            <input
              type="checkbox"
              checked={startNow}
              onChange={(e) => setStartNow(e.target.checked)}
              disabled={loading}
              className="h-3.5 w-3.5 accent-white"
            />
            Beide Services jetzt starten
          </label>

          {error && (
            <div className="animate-[fadeInUp_0.25s_ease-out_both] font-mono text-[11px] text-red-400">{error}</div>
          )}

          <div className="mt-2 flex gap-3">
            <button
              type="submit"
              disabled={loading}
              className="flex flex-1 items-center justify-center gap-2 border border-white bg-white py-2.5 font-mono text-xs font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-[0.98] disabled:opacity-60"
            >
              {loading ? (
                <span className="h-3 w-3 animate-spin border-2 border-black/30 border-t-black" />
              ) : (
                <IconCheck className="h-3.5 w-3.5" />
              )}
              Einrichten
            </button>
            <button
              type="button"
              onClick={handleSkip}
              disabled={loading}
              className="border border-white/15 px-4 py-2.5 font-mono text-xs uppercase tracking-wider text-white/50 transition-colors hover:border-white/30 hover:text-white/80 disabled:opacity-60"
            >
              Überspringen
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
