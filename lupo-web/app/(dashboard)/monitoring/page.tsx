"use client";

import { useCallback, useEffect, useRef, useState, type ComponentType, type SVGProps } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService, MonitoringSnapshot, PlayerInfo } from "@/lib/types";
import { Badge, CountUp, GlassCard, LiveDot, MetricStat, SkeletonCard, Sparkline, UsageBar, fadeStyle } from "@/components/ui";
import { IconActivity, IconCpu, IconDisk, IconMemory, IconUsers } from "@/components/icons";
import SpotlightCard from "@/components/SpotlightCard";

const HISTORY_LENGTH = 40;

function formatUptime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m ${seconds % 60}s`;
}

function formatGB(mb: number): string {
  return `${(mb / 1024).toFixed(1)} GB`;
}

function formatMemory(mb: number): string {
  return mb >= 1024 ? formatGB(mb) : `${Math.round(mb)} MB`;
}

function pushHistory(history: Map<string, number[]>, key: string, value: number): number[] {
  const next = [...(history.get(key) ?? []), value].slice(-HISTORY_LENGTH);
  history.set(key, next);
  return next;
}

function sparkData(history: Record<string, number[]>, key: string): number[] {
  const entries = history[key] ?? [];
  return entries.length >= 2 ? entries : [entries[0] ?? 0, entries[0] ?? 0];
}

export default function MonitoringPage() {
  const { token } = useAuth();
  const [snapshot, setSnapshot] = useState<MonitoringSnapshot | null>(null);
  const [services, setServices] = useState<CloudService[]>([]);
  const [players, setPlayers] = useState<PlayerInfo[]>([]);
  const [secondsAgo, setSecondsAgo] = useState(0);
  const [cpuHistory, setCpuHistory] = useState<Record<string, number[]>>({});
  const cpuHistoryRef = useRef(new Map<string, number[]>());
  const lastUpdateRef = useRef(Date.now());

  const refresh = useCallback(() => {
    if (!token) return;
    Promise.all([api.getMonitoring(token), api.listServices(token), api.listPlayers(token)])
      .then(([data, serviceList, playerList]) => {
        pushHistory(cpuHistoryRef.current, "__host__", data.host.cpuPercent);
        pushHistory(
          cpuHistoryRef.current,
          "__host_mem__",
          data.host.totalMemoryMB > 0 ? (data.host.usedMemoryMB / data.host.totalMemoryMB) * 100 : 0,
        );
        pushHistory(
          cpuHistoryRef.current,
          "__host_disk__",
          data.host.totalDiskMB > 0 ? (data.host.usedDiskMB / data.host.totalDiskMB) * 100 : 0,
        );
        for (const service of data.services) {
          pushHistory(cpuHistoryRef.current, service.name, service.cpuPercent);
        }
        setSnapshot(data);
        setServices(serviceList);
        setPlayers(playerList);
        setCpuHistory(Object.fromEntries(cpuHistoryRef.current));
        lastUpdateRef.current = Date.now();
        setSecondsAgo(0);
      })
      .catch(() => {});
  }, [token]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 3000);
    return () => clearInterval(interval);
  }, [refresh]);

  useEffect(() => {
    const tick = setInterval(() => {
      setSecondsAgo(Math.floor((Date.now() - lastUpdateRef.current) / 1000));
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  if (!snapshot) {
    return (
      <div className="flex flex-col gap-6">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <SkeletonCard className="h-24" />
          <SkeletonCard className="h-24" />
          <SkeletonCard className="h-24" />
          <SkeletonCard className="h-24" />
        </div>
        <div>
          <h1 className="mb-3 font-mono text-xs uppercase tracking-wider text-white/40">Host</h1>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <SkeletonCard className="h-32" />
            <SkeletonCard className="h-32" />
            <SkeletonCard className="h-32" />
          </div>
        </div>
        <div>
          <h2 className="mb-3 font-mono text-xs uppercase tracking-wider text-white/40">Services</h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <SkeletonCard className="h-36" />
            <SkeletonCard className="h-36" />
            <SkeletonCard className="h-36" />
          </div>
        </div>
      </div>
    );
  }

  const { host, services: serviceMetrics } = snapshot;
  const memPercent = host.totalMemoryMB > 0 ? Math.round((host.usedMemoryMB / host.totalMemoryMB) * 100) : 0;
  const diskPercent = host.totalDiskMB > 0 ? Math.round((host.usedDiskMB / host.totalDiskMB) * 100) : 0;
  const runningServices = serviceMetrics.filter((s) => s.running);
  const runningCount = runningServices.length;
  const totalServiceMemoryMB = serviceMetrics.reduce((sum, s) => sum + s.memoryMB, 0);
  const avgServiceCpu =
    runningServices.length > 0
      ? Math.round(runningServices.reduce((sum, s) => sum + s.cpuPercent, 0) / runningServices.length)
      : 0;
  const serviceMeta = new Map(services.map((s) => [s.name, s]));
  const playersByService = new Map<string, PlayerInfo[]>();
  for (const p of players) {
    const list = playersByService.get(p.serviceName) ?? [];
    list.push(p);
    playersByService.set(p.serviceName, list);
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <OverviewTile icon={IconUsers} label="Spieler online" value={players.length} index={0} />
        <OverviewTile icon={IconActivity} label="Dienste online" value={runningCount} sub={`von ${serviceMetrics.length}`} index={1} />
        <OverviewTile icon={IconCpu} label="Ø CPU (Dienste)" value={avgServiceCpu} unit="%" index={2} />
        <OverviewTile icon={IconMemory} label="RAM (Dienste)" value={null} display={formatMemory(totalServiceMemoryMB)} index={3} />
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h1 className="font-mono text-xs uppercase tracking-wider text-white/40">Host</h1>
          <div className="flex items-center gap-1.5">
            <LiveDot color="bg-[#EAB308]" />
            <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">
              Live &middot; vor {secondsAgo}s
            </span>
          </div>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <MetricStat
            icon={IconCpu}
            label="CPU"
            value={Math.round(host.cpuPercent)}
            history={sparkData(cpuHistory, "__host__")}
            style={fadeStyle(0)}
          />
          <MetricStat
            icon={IconMemory}
            label="RAM"
            value={memPercent}
            sub={`${formatGB(host.usedMemoryMB)} / ${formatGB(host.totalMemoryMB)}`}
            history={sparkData(cpuHistory, "__host_mem__")}
            style={fadeStyle(1)}
          />
          <MetricStat
            icon={IconDisk}
            label="Disk"
            value={diskPercent}
            sub={`${formatGB(host.usedDiskMB)} / ${formatGB(host.totalDiskMB)}`}
            history={sparkData(cpuHistory, "__host_disk__")}
            style={fadeStyle(2)}
          />
        </div>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-mono text-xs uppercase tracking-wider text-white/40">Services</h2>
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">
            {runningCount} / {serviceMetrics.length} online
          </span>
        </div>
        {serviceMetrics.length === 0 && (
          <GlassCard>
            <div className="px-4 py-8 text-center font-mono text-xs text-white/35">Keine Dienste aktiv.</div>
          </GlassCard>
        )}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {serviceMetrics.map((s, i) => {
            const meta = serviceMeta.get(s.name);
            const servicePlayers = playersByService.get(s.name) ?? [];
            const cpuTone = s.cpuPercent > 85 ? "#f87171" : s.cpuPercent > 65 ? "#fbbf24" : "#EAB308";
            return (
              <GlassCard
                key={s.name}
                style={fadeStyle(i)}
                className={`stagger-in p-4 hover:-translate-y-0.5 hover:shadow-[0_12px_30px_-8px_rgba(0,0,0,0.5)] ${s.running ? "" : "opacity-50"}`}
              >
                <div className="mb-3 flex items-center justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <LiveDot color={s.running ? "bg-emerald-400" : "bg-white/25"} />
                    <span className="truncate text-sm font-medium text-white">{s.name}</span>
                  </div>
                  <span className="shrink-0 font-mono text-[11px] text-white/35">
                    {s.running ? formatUptime(s.uptimeSeconds) : "Gestoppt"}
                  </span>
                </div>
                {meta && (
                  <div className="mb-3 flex items-center gap-2">
                    <Badge>{meta.type}</Badge>
                    <span className="font-mono text-[11px] text-white/35">Port {meta.port}</span>
                  </div>
                )}
                <div className="flex flex-col gap-2.5">
                  <UsageBar label="CPU" value={Math.round(s.cpuPercent)} />
                  <div className="flex items-center gap-2.5">
                    <span className="w-9 shrink-0 font-mono text-[10px] uppercase tracking-wider text-white/35">RAM</span>
                    <div className="h-1 flex-1 bg-white/10">
                      <div
                        className="h-full bg-white/70 transition-[width] duration-700 ease-out"
                        style={{
                          width: `${host.totalMemoryMB > 0 ? Math.min(100, (s.memoryMB / host.totalMemoryMB) * 100) : 0}%`,
                        }}
                      />
                    </div>
                    <span className="w-14 shrink-0 text-right font-mono text-[10px] tabular-nums text-white/45">
                      {formatMemory(s.memoryMB)}
                    </span>
                  </div>
                </div>
                <Sparkline data={sparkData(cpuHistory, s.name)} color={cpuTone} className="mt-3 h-8 w-full" />
                <div className="mt-3 flex flex-wrap items-center gap-1.5 border-t border-white/10 pt-3">
                  <IconUsers className="h-3 w-3 shrink-0 text-white/25" />
                  {servicePlayers.length === 0 ? (
                    <span className="font-mono text-[10px] text-white/25">Keine Spieler</span>
                  ) : (
                    servicePlayers.map((p) => (
                      <span
                        key={p.playerName}
                        className="stagger-in border border-white/10 bg-white/[0.03] px-1.5 py-0.5 font-mono text-[10px] text-white/60"
                      >
                        {p.playerName}
                      </span>
                    ))
                  )}
                </div>
              </GlassCard>
            );
          })}
        </div>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-mono text-xs uppercase tracking-wider text-white/40">Spieler</h2>
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">{players.length} online</span>
        </div>
        <GlassCard>
          {players.length === 0 && (
            <div className="px-4 py-8 text-center font-mono text-xs text-white/35">Niemand online.</div>
          )}
          {players.map((p, i) => (
            <div
              key={p.playerName}
              style={fadeStyle(i)}
              className={`stagger-in flex items-center justify-between gap-4 px-4 py-2.5 transition-colors duration-150 hover:bg-white/[0.03] ${i > 0 ? "border-t border-white/10" : ""}`}
            >
              <div className="flex items-center gap-2.5">
                <LiveDot color="bg-emerald-400" />
                <span className="text-sm text-white/85">{p.playerName}</span>
              </div>
              <Badge>{p.serviceName}</Badge>
            </div>
          ))}
        </GlassCard>
      </div>
    </div>
  );
}

function OverviewTile({
  icon: Icon,
  label,
  value,
  unit,
  sub,
  display,
  index,
}: {
  icon: ComponentType<SVGProps<SVGSVGElement>>;
  label: string;
  value: number | null;
  unit?: string;
  sub?: string;
  display?: string;
  index: number;
}) {
  return (
    <SpotlightCard style={fadeStyle(index)} className="stagger-in custom-spotlight-card p-4">
      <div className="mb-2 flex items-center justify-between">
        <span className="flex h-7 w-7 items-center justify-center border border-white/15 text-white/50">
          <Icon className="h-3.5 w-3.5" />
        </span>
        <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">{label}</span>
      </div>
      {display ? (
        <div className="font-minecraft text-3xl leading-none text-white">{display}</div>
      ) : (
        <div className="flex items-baseline gap-1 font-minecraft text-3xl leading-none text-white">
          <CountUp value={value ?? 0} />
          {unit && <span className="text-sm text-white/30">{unit}</span>}
        </div>
      )}
      {sub && <div className="mt-1 font-mono text-[10px] text-white/35">{sub}</div>}
    </SpotlightCard>
  );
}
