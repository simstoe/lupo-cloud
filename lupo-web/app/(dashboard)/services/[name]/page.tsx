"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService } from "@/lib/types";

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
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">{name}</h1>
          {service && (
            <p className="text-sm text-neutral-400">
              {service.type} &middot; port {service.port} &middot;{" "}
              <span className={service.running ? "text-green-400" : "text-neutral-500"}>
                {service.running ? "running" : "stopped"}
              </span>
            </p>
          )}
        </div>
        <button
          onClick={handleStop}
          disabled={!service?.running}
          className="rounded border border-red-800 px-3 py-1.5 text-sm text-red-400 hover:bg-red-950 disabled:opacity-50 cursor-pointer"
        >
          Stop
        </button>
      </div>

      <div className="mb-4 flex gap-4 border-b border-neutral-800 text-sm">
        <button
          onClick={() => setTab("console")}
          className={`cursor-pointer border-b-2 px-1 pb-2 ${
            tab === "console" ? "border-blue-500 text-white" : "border-transparent text-neutral-400"
          }`}
        >
          Console
        </button>
        <button
          onClick={() => setTab("backups")}
          className={`cursor-pointer border-b-2 px-1 pb-2 ${
            tab === "backups" ? "border-blue-500 text-white" : "border-transparent text-neutral-400"
          }`}
        >
          Backups
        </button>
      </div>

      {tab === "console" ? <Console name={name} token={token} /> : <Backups name={name} token={token} />}
    </div>
  );
}

function Console({ name, token }: { name: string; token: string | null }) {
  const [lines, setLines] = useState<string[]>([]);
  const [command, setCommand] = useState("");
  const [connected, setConnected] = useState(false);
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

  return (
    <div>
      <div
        ref={scrollRef}
        className="mb-3 h-96 overflow-y-auto rounded-lg border border-neutral-800 bg-black p-3 font-mono text-xs text-neutral-300"
      >
        {lines.length === 0 && <p className="text-neutral-600">{connected ? "No log output yet." : "Connecting..."}</p>}
        {lines.map((line, i) => (
          <div key={i} className="whitespace-pre-wrap">
            {line}
          </div>
        ))}
      </div>
      <form onSubmit={sendCommand} className="flex gap-2">
        <input
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          placeholder="Send a command..."
          disabled={!connected}
          className="flex-1 rounded border border-neutral-700 bg-neutral-950 px-3 py-2 font-mono text-sm outline-none focus:border-neutral-500 disabled:opacity-50"
        />
        <button
          type="submit"
          disabled={!connected}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
        >
          Send
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
    <div>
      <button
        onClick={handleCreate}
        disabled={busy === "__create__"}
        className="mb-4 rounded bg-blue-600 px-3 py-1.5 text-sm font-medium hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
      >
        Create backup
      </button>

      <ul className="divide-y divide-neutral-800 rounded-lg border border-neutral-800">
        {backups.length === 0 && <li className="px-4 py-6 text-center text-sm text-neutral-500">No backups yet.</li>}
        {backups.map((file) => (
          <li key={file} className="flex items-center justify-between px-4 py-2 text-sm">
            <span className="font-mono text-neutral-300">{file}</span>
            <div className="flex gap-2">
              <button
                onClick={() => handleRestore(file)}
                disabled={busy === file}
                className="rounded border border-neutral-700 px-2 py-1 text-xs hover:bg-neutral-800 disabled:opacity-50 cursor-pointer"
              >
                Restore
              </button>
              <button
                onClick={() => handleDelete(file)}
                disabled={busy === file}
                className="rounded border border-red-800 px-2 py-1 text-xs text-red-400 hover:bg-red-950 disabled:opacity-50 cursor-pointer"
              >
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
