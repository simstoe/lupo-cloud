"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { ServiceTask, ServiceType } from "@/lib/types";
import { GlassCard, Badge, fadeStyle } from "@/components/ui";

const emptyForm = (): ServiceTask => ({
  name: "",
  type: "PAPER",
  templateName: null,
  minMemoryMB: 512,
  maxMemoryMB: 1024,
  cpuCoreLimit: null,
  startPort: 25565,
  minOnlineCount: 1,
  maxOnlineCount: 1,
  staticService: false,
  autostart: false,
  group: null,
  proxyGroups: null,
});

export default function TasksPage() {
  const { token } = useAuth();
  const [tasks, setTasks] = useState<ServiceTask[]>([]);
  const [editing, setEditing] = useState<ServiceTask | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const refresh = useCallback(() => {
    if (!token) return;
    api.listTasks(token).then(setTasks).catch(() => {});
  }, [token]);

  useEffect(refresh, [refresh]);

  async function handleDelete(name: string) {
    if (!token) return;
    setBusy(name);
    try {
      await api.deleteTask(token, name);
      refresh();
    } finally {
      setBusy(null);
    }
  }

  async function handleStart(name: string) {
    if (!token) return;
    setBusy(name);
    try {
      await api.startTask(token, name);
    } finally {
      setBusy(null);
    }
  }

  async function handleSave(task: ServiceTask, isNew: boolean) {
    if (!token) return;
    if (isNew) {
      await api.createTask(token, task);
    } else {
      await api.updateTask(token, task.name, task);
    }
    setEditing(null);
    refresh();
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="font-mono text-xs uppercase tracking-wider text-white/40">Aufgaben</h1>
        <button
          onClick={() => setEditing(emptyForm())}
          className="border border-white bg-white px-3 py-1.5 font-mono text-[11px] font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95"
        >
          Neue Aufgabe
        </button>
      </div>

      <GlassCard>
        {tasks.length === 0 && (
          <div className="px-4 py-8 text-center font-mono text-xs text-white/35">Noch keine Aufgaben.</div>
        )}
        {tasks.map((t, i) => (
          <div
            key={t.name}
            style={fadeStyle(i)}
            className={`stagger-in flex items-center justify-between gap-4 px-4 py-3 transition-colors duration-150 hover:bg-white/[0.03] ${i > 0 ? "border-t border-white/10" : ""}`}
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="truncate text-sm font-medium text-white">{t.name}</span>
                <Badge>{t.type}</Badge>
              </div>
              <div className="mt-0.5 font-mono text-[11px] text-white/35">
                Port {t.startPort} · {t.minMemoryMB}–{t.maxMemoryMB}MB
                {t.staticService ? " · statisch" : ""}
                {t.autostart ? " · autostart" : ""}
              </div>
            </div>
            <div className="flex shrink-0 gap-2">
              <button
                onClick={() => handleStart(t.name)}
                disabled={busy === t.name}
                className="border border-white/15 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95 disabled:opacity-50"
              >
                Starten
              </button>
              <button
                onClick={() => setEditing(t)}
                className="border border-white/15 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95"
              >
                Bearbeiten
              </button>
              <button
                onClick={() => handleDelete(t.name)}
                disabled={busy === t.name}
                className="border border-red-900/60 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
              >
                Löschen
              </button>
            </div>
          </div>
        ))}
      </GlassCard>

      {editing && (
        <TaskForm
          initial={editing}
          isNew={tasks.every((t) => t.name !== editing.name) || editing.name === ""}
          onCancel={() => setEditing(null)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}

function TaskForm({
  initial,
  isNew,
  onCancel,
  onSave,
}: {
  initial: ServiceTask;
  isNew: boolean;
  onCancel: () => void;
  onSave: (task: ServiceTask, isNew: boolean) => void;
}) {
  const [task, setTask] = useState<ServiceTask>(initial);

  function set<K extends keyof ServiceTask>(key: K, value: ServiceTask[K]) {
    setTask((t) => ({ ...t, [key]: value }));
  }

  return (
    <GlassCard className="animate-[scaleIn_0.2s_ease-out_both] p-5">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onSave(task, isNew);
        }}
      >
        <h2 className="mb-4 font-mono text-xs uppercase tracking-wider text-white/40">
          {isNew ? "Neue Aufgabe" : `Bearbeite ${initial.name}`}
        </h2>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Name">
            <input
              required
              disabled={!isNew}
              value={task.name}
              onChange={(e) => set("name", e.target.value)}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
            />
          </Field>
          <Field label="Typ">
            <select
              value={task.type}
              onChange={(e) => set("type", e.target.value as ServiceType)}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            >
              <option value="PAPER">PAPER</option>
              <option value="VELOCITY">VELOCITY</option>
            </select>
          </Field>
          <Field label="Vorlagenname">
            <input
              value={task.templateName ?? ""}
              onChange={(e) => set("templateName", e.target.value || null)}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Gruppe">
            <input
              value={task.group ?? ""}
              onChange={(e) => set("group", e.target.value || null)}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Startport">
            <input
              type="number"
              value={task.startPort}
              onChange={(e) => set("startPort", Number(e.target.value))}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="CPU-Kernlimit">
            <input
              type="number"
              value={task.cpuCoreLimit ?? ""}
              onChange={(e) => set("cpuCoreLimit", e.target.value ? Number(e.target.value) : null)}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Min. Speicher (MB)">
            <input
              type="number"
              value={task.minMemoryMB}
              onChange={(e) => set("minMemoryMB", Number(e.target.value))}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Max. Speicher (MB)">
            <input
              type="number"
              value={task.maxMemoryMB}
              onChange={(e) => set("maxMemoryMB", Number(e.target.value))}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Min. Online">
            <input
              type="number"
              value={task.minOnlineCount}
              onChange={(e) => set("minOnlineCount", Number(e.target.value))}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Max. Online">
            <input
              type="number"
              value={task.maxOnlineCount}
              onChange={(e) => set("maxOnlineCount", Number(e.target.value))}
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
          <Field label="Proxy-Gruppen (kommagetrennt)">
            <input
              value={task.proxyGroups?.join(",") ?? ""}
              onChange={(e) =>
                set("proxyGroups", e.target.value ? e.target.value.split(",").map((s) => s.trim()) : null)
              }
              className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
            />
          </Field>
        </div>

        <div className="mt-4 flex gap-6">
          <label className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-wider text-white/50">
            <input
              type="checkbox"
              checked={task.staticService}
              onChange={(e) => set("staticService", e.target.checked)}
            />
            Statisch
          </label>
          <label className="flex items-center gap-2 font-mono text-[11px] uppercase tracking-wider text-white/50">
            <input type="checkbox" checked={task.autostart} onChange={(e) => set("autostart", e.target.checked)} />
            Autostart
          </label>
        </div>

        <div className="mt-6 flex gap-2">
          <button
            type="submit"
            className="border border-white bg-white px-3 py-1.5 font-mono text-[11px] font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95"
          >
            Speichern
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="border border-white/15 px-3 py-1.5 font-mono text-[11px] uppercase tracking-wider text-white/70 transition-all duration-150 hover:bg-white/10 hover:text-white active:scale-95"
          >
            Abbrechen
          </button>
        </div>
      </form>
    </GlassCard>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">{label}</span>
      {children}
    </label>
  );
}
