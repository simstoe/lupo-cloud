"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService, ServiceTask } from "@/lib/types";
import { GlassCard, Badge, LiveDot, CountUp, fadeStyle } from "@/components/ui";
import SpotlightCard from "@/components/SpotlightCard";
import SpecularButton from "@/components/SpecularButton";

export default function DashboardPage() {
  const { token } = useAuth();
  const [services, setServices] = useState<CloudService[] | null>(null);
  const [tasks, setTasks] = useState<ServiceTask[]>([]);
  const [busy, setBusy] = useState<string | null>(null);

  const refreshServices = useCallback(() => {
    if (!token) return;
    api.listServices(token).then(setServices).catch(() => {});
  }, [token]);

  useEffect(() => {
    if (!token) return;
    refreshServices();
    api.listTasks(token).then(setTasks).catch(() => {});
    const interval = setInterval(refreshServices, 3000);
    return () => clearInterval(interval);
  }, [token, refreshServices]);

  async function handleStop(name: string) {
    if (!token) return;
    setBusy(name);
    try {
      await api.stopService(token, name);
      refreshServices();
    } finally {
      setBusy(null);
    }
  }

  async function handleStartTask(name: string) {
    if (!token) return;
    setBusy(name);
    try {
      await api.startTask(token, name);
      setTimeout(refreshServices, 1000);
    } finally {
      setBusy(null);
    }
  }

  async function handleStopAll() {
    if (!token) return;
    setBusy("__all__");
    try {
      await api.stopAllServices(token);
      refreshServices();
    } finally {
      setBusy(null);
    }
  }

  const running = services?.filter((s) => s.running).length ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <SpotlightCard className="stagger-in p-4 custom-spotlight-card">
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">Online</span>
          <div className="-mt-2 flex items-center gap-2">
            <CountUp value={running} className="font-minecraft text-6xl text-white" />
          </div>
        </SpotlightCard>
        <SpotlightCard className="stagger-in p-4 custom-spotlight-card">
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">Services</span>
          <CountUp value={services?.length ?? 0} className="-mt-2 font-minecraft block text-6xl text-white" />
        </SpotlightCard>
        <SpotlightCard className="stagger-in p-4 custom-spotlight-card">
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">Aufgaben</span>
          <CountUp value={tasks.length} className="-mt-2 block font-minecraft text-6xl text-white" />
        </SpotlightCard>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h1 className="font-mono text-xs uppercase tracking-wider text-white/40">Running services</h1>
          <button
            onClick={handleStopAll}
            disabled={busy === "__all__"}
            className="border border-red-900/60 px-3 py-1.5 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
          >
            Alle stoppen
          </button>
        </div>

        <GlassCard>
          {services?.length === 0 && (
            <div className="px-4 py-8 text-center font-mono text-xs text-white/35">No service active.</div>
          )}
          {services?.map((s, i) => (
            <div
              key={s.uuid}
              style={fadeStyle(i)}
              className={`stagger-in flex items-center justify-between gap-4 px-4 py-3 transition-colors duration-150 hover:bg-white/[0.03] ${i > 0 ? "border-t border-white/10" : ""}`}
            >
              <div className="flex min-w-0 items-center gap-3">
                <LiveDot color={s.running ? "bg-emerald-400" : "bg-white/25"} />
                <div className="min-w-0">
                  <Link
                    href={`/services/${encodeURIComponent(s.name)}`}
                    className="truncate text-sm font-medium text-white transition-colors hover:text-white/80 hover:underline"
                  >
                    {s.name}
                  </Link>
                  <div className="font-mono text-[11px] text-white/35">Port {s.port}</div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Badge>{s.type}</Badge>
                <button
                  onClick={() => handleStop(s.name)}
                  disabled={busy === s.name}
                  className="border border-white/15 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95 disabled:opacity-50"
                >
                  Stop
                </button>
              </div>
            </div>
          ))}
        </GlassCard>
      </div>

      <div>
        <h2 className="mb-3 font-mono text-xs uppercase tracking-wider text-white/40">Aus Aufgabe starten</h2>
        {tasks.length === 0 ? (
          <p className="font-mono text-xs text-white/35">Noch keine Aufgaben konfiguriert.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {tasks.map((t, i) => (
                <SpecularButton
                    size="sm"
                    radius={0}
                    tint="#ffffff"
                    tintOpacity={0}
                    blur={0}
                    textColor="#f5f5f5"
                    lineColor="#ffffff"
                    baseColor="#525252"
                    intensity={1}
                    shineSize={10}
                    shineFade={40}
                    thickness={1}
                    speed={0.35}
                    followMouse
                    proximity={250}
                    autoAnimate={false}
                    key={t.name}
                    disabled={busy === t.name}
                    onClick={() => handleStartTask(t.name)}
                >
                  Start a {t.name} service
                </SpecularButton>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
