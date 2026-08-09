"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService, ServiceTask } from "@/lib/types";

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

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Services</h1>
        <button
          onClick={handleStopAll}
          disabled={busy === "__all__"}
          className="rounded border border-red-800 px-3 py-1.5 text-sm text-red-400 hover:bg-red-950 disabled:opacity-50 cursor-pointer"
        >
          Stop all
        </button>
      </div>

      <table className="w-full border-collapse overflow-hidden rounded-lg border border-neutral-800 text-sm">
        <thead className="bg-neutral-900 text-left text-neutral-400">
          <tr>
            <th className="px-4 py-2 font-medium">Name</th>
            <th className="px-4 py-2 font-medium">Type</th>
            <th className="px-4 py-2 font-medium">Port</th>
            <th className="px-4 py-2 font-medium">Status</th>
            <th className="px-4 py-2 font-medium"></th>
          </tr>
        </thead>
        <tbody>
          {services?.length === 0 && (
            <tr>
              <td colSpan={5} className="px-4 py-6 text-center text-neutral-500">
                No services running.
              </td>
            </tr>
          )}
          {services?.map((s) => (
            <tr key={s.uuid} className="border-t border-neutral-800">
              <td className="px-4 py-2">
                <Link href={`/services/${encodeURIComponent(s.name)}`} className="text-blue-400 hover:underline">
                  {s.name}
                </Link>
              </td>
              <td className="px-4 py-2 text-neutral-400">{s.type}</td>
              <td className="px-4 py-2 text-neutral-400">{s.port}</td>
              <td className="px-4 py-2">
                <span
                  className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs ${
                    s.running ? "bg-green-950 text-green-400" : "bg-neutral-800 text-neutral-400"
                  }`}
                >
                  <span className={`h-1.5 w-1.5 rounded-full ${s.running ? "bg-green-400" : "bg-neutral-500"}`} />
                  {s.running ? "running" : "stopped"}
                </span>
              </td>
              <td className="px-4 py-2 text-right">
                <button
                  onClick={() => handleStop(s.name)}
                  disabled={busy === s.name}
                  className="rounded border border-neutral-700 px-2 py-1 text-xs hover:bg-neutral-800 disabled:opacity-50 cursor-pointer"
                >
                  Stop
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 className="mt-8 mb-3 text-sm font-medium text-neutral-400">Start from task</h2>
      <div className="flex flex-wrap gap-2">
        {tasks.length === 0 && <p className="text-sm text-neutral-500">No tasks configured yet.</p>}
        {tasks.map((t) => (
          <button
            key={t.name}
            onClick={() => handleStartTask(t.name)}
            disabled={busy === t.name}
            className="rounded border border-neutral-700 px-3 py-1.5 text-sm hover:bg-neutral-800 disabled:opacity-50 cursor-pointer"
          >
            Start {t.name}
          </button>
        ))}
      </div>
    </div>
  );
}
