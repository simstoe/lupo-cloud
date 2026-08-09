"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { CloudService, TemplateDto } from "@/lib/types";
import { GlassCard, Badge, fadeStyle } from "@/components/ui";

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
    <div className="flex flex-col gap-6">
      <h1 className="font-mono text-xs uppercase tracking-wider text-white/40">Vorlagen</h1>

      <GlassCard>
        {templates.length === 0 && (
          <div className="px-4 py-8 text-center font-mono text-xs text-white/35">Noch keine Vorlagen.</div>
        )}
        {templates.map((t, i) => (
          <div
            key={t.name}
            style={fadeStyle(i)}
            className={`stagger-in flex items-center justify-between gap-4 px-4 py-3 transition-colors duration-150 hover:bg-white/[0.03] ${i > 0 ? "border-t border-white/10" : ""}`}
          >
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-white">{t.name}</span>
              <Badge>{t.type}</Badge>
            </div>
            <button
              onClick={() => handleDelete(t.name)}
              disabled={busy === t.name}
              className="border border-red-900/60 px-2.5 py-1 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
            >
              Löschen
            </button>
          </div>
        ))}
      </GlassCard>

      <GlassCard className="p-5">
        <form onSubmit={handleSave}>
          <h2 className="mb-4 font-mono text-xs uppercase tracking-wider text-white/40">
            Vorlage aus laufendem Dienst speichern
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Vorlagenname</span>
              <input
                required
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Quelldienst</span>
              <select
                required
                value={sourceService}
                onChange={(e) => setSourceService(e.target.value)}
                className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
              >
                <option value="" disabled>
                  Dienst auswählen
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
            className="mt-4 border border-white bg-white px-3 py-1.5 font-mono text-[11px] font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
          >
            Vorlage speichern
          </button>
        </form>
      </GlassCard>
    </div>
  );
}
