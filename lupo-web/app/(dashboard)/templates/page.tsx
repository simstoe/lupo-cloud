"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService, TemplateDto } from "@/lib/types";

export default function TemplatesPage() {
  const { token } = useAuth();
  const [templates, setTemplates] = useState<TemplateDto[]>([]);
  const [services, setServices] = useState<CloudService[]>([]);
  const [templateName, setTemplateName] = useState("");
  const [sourceService, setSourceService] = useState("");
  const [busy, setBusy] = useState<string | null>(null);

  const refresh = useCallback(() => {
    if (!token) return;
    api.listTemplates(token).then(setTemplates).catch(() => {});
  }, [token]);

  useEffect(() => {
    refresh();
    if (token) api.listServices(token).then(setServices).catch(() => {});
  }, [refresh, token]);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!token || !templateName || !sourceService) return;
    setBusy("__save__");
    try {
      await api.saveTemplate(token, templateName, sourceService);
      setTemplateName("");
      setSourceService("");
      refresh();
    } finally {
      setBusy(null);
    }
  }

  async function handleDelete(name: string) {
    if (!token) return;
    setBusy(name);
    try {
      await api.deleteTemplate(token, name);
      refresh();
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-6 text-xl font-semibold">Templates</h1>

      <ul className="mb-6 divide-y divide-neutral-800 rounded-lg border border-neutral-800">
        {templates.length === 0 && (
          <li className="px-4 py-6 text-center text-sm text-neutral-500">No templates yet.</li>
        )}
        {templates.map((t) => (
          <li key={t.name} className="flex items-center justify-between px-4 py-2 text-sm">
            <span>
              <span className="font-medium">{t.name}</span> <span className="text-neutral-500">{t.type}</span>
            </span>
            <button
              onClick={() => handleDelete(t.name)}
              disabled={busy === t.name}
              className="rounded border border-red-800 px-2 py-1 text-xs text-red-400 hover:bg-red-950 disabled:opacity-50 cursor-pointer"
            >
              Delete
            </button>
          </li>
        ))}
      </ul>

      <form onSubmit={handleSave} className="rounded-lg border border-neutral-800 bg-neutral-900 p-4">
        <h2 className="mb-3 font-medium">Save template from a running service</h2>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <label className="block">
            <span className="mb-1 block text-xs text-neutral-400">Template name</span>
            <input
              required
              value={templateName}
              onChange={(e) => setTemplateName(e.target.value)}
              className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-neutral-400">Source service</span>
            <select
              required
              value={sourceService}
              onChange={(e) => setSourceService(e.target.value)}
              className="w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5 outline-none focus:border-neutral-500"
            >
              <option value="" disabled>
                Select a service
              </option>
              {services.map((s) => (
                <option key={s.uuid} value={s.name}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <button
          type="submit"
          disabled={busy === "__save__"}
          className="mt-4 rounded bg-blue-600 px-3 py-1.5 text-sm font-medium hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
        >
          Save template
        </button>
      </form>
    </div>
  );
}
