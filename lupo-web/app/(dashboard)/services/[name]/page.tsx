"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService } from "@/lib/types";
import { GlassCard, Badge, LiveDot, fadeStyle } from "@/components/ui";
import { IconCopy, IconCheck } from "@/components/icons";

export default function ServiceDetailPage() {
  const { name } = useParams<{ name: string }>();
  const { token } = useAuth();
  const [service, setService] = useState<CloudService | null>(null);
  const [tab, setTab] = useState<"console" | "backups">("console");

  const refreshService = useCallback(() => {
    if (!token) return;
    api
      .getService(token, name)
      .then(setService)
      .catch(() => setService(null));
  }, [token, name]);

  useEffect(() => {
    refreshService();
    const interval = setInterval(refreshService, 3000);
    return () => clearInterval(interval);
  }, [refreshService]);

  async function handleStop() {
    if (!token) return;
    await api.stopService(token, name);
    refreshService();
  }

  return (
    <div className="flex flex-col gap-6">
      <GlassCard className="flex items-center justify-between gap-4 p-4">
        <div className="flex items-center gap-3">
          <LiveDot color={service?.running ? "bg-emerald-400" : "bg-white/25"} />
          <div>
            <h1 className="text-base font-semibold text-white">{ name }</h1>
            {service && (
              <div className="mt-0.5 flex items-center gap-2 font-mono text-[11px] text-white/40">
                <Badge>{service.type}</Badge>
                <span>Port {service.port}</span>
                <span className={service.running ? "text-emerald-400" : "text-white/35"}>
                  {service.running ? "online" : "gestoppt"}
                </span>
              </div>
            )}
          </div>
        </div>
        <button
          onClick={handleStop}
          disabled={!service?.running}
          className="border border-red-900/60 px-3 py-1.5 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
        >
          Stop
        </button>
      </GlassCard>

      <div className="flex gap-6 border-b border-white/10 font-mono text-[11px] uppercase tracking-wider">
        {(["console", "backups"] as const).map((id) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`relative px-1 pb-2 transition-colors duration-200 ${
              tab === id ? "text-white" : "text-white/35 hover:text-white/60"
            }`}
          >
            {id === "console" ? "Konsole" : "Backups"}
            {tab === id && (
              <span className="absolute inset-x-0 -bottom-px h-0.5 origin-left animate-[underlineIn_0.25s_ease-out_both] bg-white" />
            )}
          </button>
        ))}
      </div>

      <div key={tab} className="page-in">
        {tab === "console" ? <Console name={name} token={token} /> : <Backups name={name} token={token} />}
      </div>
    </div>
  );
}

function Console({ name, token }: { name: string; token: string | null }) {
  const [lines, setLines] = useState<string[]>([]);
  const [command, setCommand] = useState("");
  const [connected, setConnected] = useState(false);
  const [copied, setCopied] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!token) return;

    const ws = new WebSocket(api.wsUrl(`/ws/console/${encodeURIComponent(name)}`, token));
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);
    ws.onclose = () => setConnected(false);
    ws.onmessage = (event) => {
      try {
        setLines(JSON.parse(event.data));
      } catch {

      }
    };

    return () => ws.close();
  }, [name, token]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [lines]);

  function sendCommand(e: React.FormEvent) {
    e.preventDefault();
    if (!command.trim() || wsRef.current?.readyState !== WebSocket.OPEN) return;
    wsRef.current.send(command);
    setCommand("");
  }

  async function copyAllLogs() {
    if (lines.length === 0) return;
    await navigator.clipboard.writeText(lines.join("\n"));
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <LiveDot color={connected ? "bg-emerald-400" : "bg-white/25"} />
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">
            {connected ? "Verbunden" : "Verbinde…"}
          </span>
        </div>
        <button
          onClick={copyAllLogs}
          disabled={lines.length === 0}
          className="flex items-center gap-1.5 border border-white/15 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95 disabled:opacity-40"
        >
          {copied ? <IconCheck className="h-3 w-3" /> : <IconCopy className="h-3 w-3" />}
          {copied ? "Kopiert" : "Alle Logs kopieren"}
        </button>
      </div>

      <div
        ref={scrollRef}
        className="custom-scrollbar h-96 overflow-y-auto border border-white/10 p-3 font-mono text-xs text-white/70"
      >
        {lines.length === 0 && <p className="text-white/25">Noch keine Log-Ausgabe.</p>}
        {lines.map((line, i) => (
          <div key={i} className="whitespace-pre-wrap">
            { line }
          </div>
        ))}
      </div>
      <form onSubmit={sendCommand} className="flex gap-2">
        <input
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          placeholder="Befehl senden…"
          disabled={!connected}
          className="flex-1 border border-white/15 bg-white/[0.02] px-3 py-2 font-mono text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
        />
        <button
          type="submit"
          disabled={!connected}
          className="border border-white bg-white px-4 py-2 font-mono text-xs font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
        >
          Senden
        </button>
      </form>
    </div>
  );
}

function Backups({ name, token }: { name: string; token: string | null }) {
  const [backups, setBackups] = useState<string[]>([]);
  const [busy, setBusy] = useState<string | null>(null);

  const refresh = useCallback(() => {
    if (!token) return;
    api.listBackups(token, name).then(setBackups).catch(() => {});
  }, [token, name]);

  useEffect(refresh, [refresh]);

  async function handleCreate() {
    if (!token) return;
    setBusy("__create__");
    try {
      await api.createBackup(token, name);
      refresh();
    } finally {
      setBusy(null);
    }
  }

  async function handleRestore(file: string) {
    if (!token) return;
    setBusy(file);
    try {
      await api.restoreBackup(token, name, file);
    } finally {
      setBusy(null);
    }
  }

  async function handleDelete(file: string) {
    if (!token) return;
    setBusy(file);
    try {
      await api.deleteBackup(token, name, file);
      refresh();
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <button
        onClick={handleCreate}
        disabled={busy === "__create__"}
        className="self-start border border-white bg-white px-3 py-1.5 font-mono text-[11px] font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
      >
        Backup erstellen
      </button>

      <GlassCard>
        {backups.length === 0 && (
          <div className="px-4 py-8 text-center font-mono text-xs text-white/35">Noch keine Backups.</div>
        )}
        {backups.map((file, i) => (
          <div
            key={file}
            style={fadeStyle(i)}
            className={`stagger-in flex items-center justify-between gap-4 px-4 py-3 transition-colors duration-150 hover:bg-white/[0.03] ${i > 0 ? "border-t border-white/10" : ""}`}
          >
            <span className="truncate font-mono text-xs text-white/70">{file}</span>
            <div className="flex shrink-0 gap-2">
              <button
                onClick={() => handleRestore(file)}
                disabled={busy === file}
                className="border border-white/15 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95 disabled:opacity-50"
              >
                Wiederherstellen
              </button>
              <button
                onClick={() => handleDelete(file)}
                disabled={busy === file}
                className="border border-red-900/60 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
              >
                Löschen
              </button>
            </div>
          </div>
        ))}
      </GlassCard>
    </div>
  );
}
