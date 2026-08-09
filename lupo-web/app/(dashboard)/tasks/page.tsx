"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { ServiceTask, ServiceType } from "@/lib/types";

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
    <div className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Tasks</h1>
        <button
          onClick={() => setEditing(emptyForm())}
          className="rounded bg-blue-600 px-3 py-1.5 text-sm font-medium hover:bg-blue-500 cursor-pointer"
        >
          New task
        </button>
      </div>

      <ul className="mb-6 divide-y divide-neutral-800 rounded-lg border border-neutral-800">
        {tasks.length === 0 && <li className="px-4 py-6 text-center text-sm text-neutral-500">No tasks yet.</li>}
        {tasks.map((t) => (
          <li key={t.name} className="flex items-center justify-between px-4 py-2 text-sm">
            <div>
              <span className="font-medium">{t.name}</span>{" "}
              <span className="text-neutral-500">
                {t.type} &middot; port {t.startPort} &middot; {t.minMemoryMB}-{t.maxMemoryMB}MB
                {t.staticService ? " · static" : ""}
                {t.autostart ? " · autostart" : ""}
              </span>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handleStart(t.name)}
                disabled={busy === t.name}
                className="rounded border border-neutral-700 px-2 py-1 text-xs hover:bg-neutral-800 disabled:opacity-50 cursor-pointer"
              >
                Start now
              </button>
              <button
                onClick={() => setEditing(t)}
                className="rounded border border-neutral-700 px-2 py-1 text-xs hover:bg-neutral-800 cursor-pointer"
              >
                Edit
              </button>
              <button
                onClick={() => handleDelete(t.name)}
                disabled={busy === t.name}
                className="rounded border border-red-800 px-2 py-1 text-xs text-red-400 hover:bg-red-950 disabled:opacity-50 cursor-pointer"
              >
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>

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
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSave(task, isNew);
      }}
      className="rounded-lg border border-neutral-800 bg-neutral-900 p-4"
    >
      <h2 className="mb-3 font-medium">{isNew ? "New task" : `Edit ${initial.name}`}</h2>

      <div className="grid grid-cols-2 gap-3 text-sm">
        <Field label="Name">
          <input
            required
            disabled={!isNew}
            value={task.name}
            onChange={(e) => set("name", e.target.value)}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Type">
          <select
            value={task.type}
            onChange={(e) => set("type", e.target.value as ServiceType)}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          >
            <option value="PAPER">PAPER</option>
            <option value="VELOCITY">VELOCITY</option>
          </select>
        </Field>
        <Field label="Template name">
          <input
            value={task.templateName ?? ""}
            onChange={(e) => set("templateName", e.target.value || null)}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Group">
          <input value={task.group ?? ""} onChange={(e) => set("group", e.target.value || null)} className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50" />
        </Field>
        <Field label="Start port">
          <input
            type="number"
            value={task.startPort}
            onChange={(e) => set("startPort", Number(e.target.value))}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="CPU core limit">
          <input
            type="number"
            value={task.cpuCoreLimit ?? ""}
            onChange={(e) => set("cpuCoreLimit", e.target.value ? Number(e.target.value) : null)}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Min memory (MB)">
          <input
            type="number"
            value={task.minMemoryMB}
            onChange={(e) => set("minMemoryMB", Number(e.target.value))}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Max memory (MB)">
          <input
            type="number"
            value={task.maxMemoryMB}
            onChange={(e) => set("maxMemoryMB", Number(e.target.value))}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Min online">
          <input
            type="number"
            value={task.minOnlineCount}
            onChange={(e) => set("minOnlineCount", Number(e.target.value))}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Max online">
          <input
            type="number"
            value={task.maxOnlineCount}
            onChange={(e) => set("maxOnlineCount", Number(e.target.value))}
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
        <Field label="Proxy groups (comma-separated)">
          <input
            value={task.proxyGroups?.join(",") ?? ""}
            onChange={(e) =>
              set("proxyGroups", e.target.value ? e.target.value.split(",").map((s) => s.trim()) : null)
            }
            className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500 disabled:opacity-50"
          />
        </Field>
      </div>

      <div className="mt-3 flex gap-4 text-sm">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={task.staticService}
            onChange={(e) => set("staticService", e.target.checked)}
          />
          Static
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={task.autostart} onChange={(e) => set("autostart", e.target.checked)} />
          Autostart
        </label>
      </div>

      <div className="mt-4 flex gap-2">
        <button type="submit" className="rounded bg-blue-600 px-3 py-1.5 text-sm font-medium hover:bg-blue-500 cursor-pointer">
          Save
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded border border-neutral-700 px-3 py-1.5 text-sm hover:bg-neutral-800 cursor-pointer"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs text-neutral-400">{label}</span>
      {children}
    </label>
  );
}
