"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { MemoryBudget, NetworkSettings } from "@/lib/types";
import { GlassCard } from "@/components/ui";
import MemoryBudgetPicker, { formatMB } from "@/components/MemoryBudgetPicker";

export default function SettingsPage() {
  const { token } = useAuth();
  const [networkName, setNetworkName] = useState("");
  const [motd, setMotd] = useState("");
  const [maxMemoryMB, setMaxMemoryMB] = useState<number | null>(null);
  const [budget, setBudget] = useState<MemoryBudget | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const refresh = useCallback(() => {
    if (!token) return;
    api.getSettings(token).then((settings: NetworkSettings) => {
      setNetworkName(settings.networkName);
      setMotd(settings.motd);
      setMaxMemoryMB(settings.maxMemoryMB);
    }).catch(() => {});
    api.getMemoryBudget(token).then(setBudget).catch(() => {});
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    setBusy("__save__");
    setSaved(false);
    try {
      await api.updateSettings(token, { networkName, motd, maxMemoryMB });
      setSaved(true);
    } finally {
      setBusy(null);
    }
  }

  async function handleDeleteAllTemplates() {
    if (!token) return;
    if (!window.confirm("Wirklich ALLE Vorlagen unwiderruflich löschen?")) return;
    setBusy("__delete-templates__");
    try {
      await api.deleteAllTemplates(token);
    } finally {
      setBusy(null);
    }
  }

  async function handleResetCloud() {
    if (!token) return;
    if (!window.confirm("Wirklich die GESAMTE Cloud zurücksetzen? Alle Server- und Proxy-Daten werden gestoppt und gelöscht. Dies kann nicht rückgängig gemacht werden.")) return;
    if (!window.confirm("Bist du dir absolut sicher? Dieser Schritt löscht alle Server- und Proxy-Verzeichnisse.")) return;
    setBusy("__reset-cloud__");
    try {
      await api.resetCloud(token);
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <GlassCard className="p-5">
        <form onSubmit={handleSave}>
          <h2 className="mb-4 font-mono text-xs uppercase tracking-wider text-white/40">Netzwerk</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Netzwerkname</span>
              <input
                required
                value={networkName}
                onChange={(e) => setNetworkName(e.target.value)}
                className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">MOTD</span>
              <input
                required
                value={motd}
                onChange={(e) => setMotd(e.target.value)}
                className="w-full border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40"
              />
            </label>
          </div>
          {budget && (
            <div className="mt-6 border-t border-white/10 pt-5">
              <div className="mb-3 flex items-baseline justify-between gap-3">
                <h3 className="font-mono text-xs uppercase tracking-wider text-white/40">RAM-Budget</h3>
                <span className="font-mono text-[11px] text-white/50">
                  {budget.committedMB} MB von {formatMB(maxMemoryMB)} belegt
                </span>
              </div>
              <p className="mb-4 font-mono text-[11px] leading-relaxed text-white/35">
                Services, die dieses Limit überschreiten würden, werden nicht gestartet.
              </p>
              <MemoryBudgetPicker
                hostTotalMB={budget.hostTotalMB}
                value={maxMemoryMB}
                onChange={setMaxMemoryMB}
                disabled={busy === "__save__"}
              />
            </div>
          )}

          <div className="mt-4 flex items-center gap-3">
            <button
              type="submit"
              disabled={busy === "__save__"}
              className="border border-white bg-white px-3 py-1.5 font-mono text-[11px] font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
            >
              Speichern
            </button>
            {saved && (
              <span className="animate-[fadeInUp_0.3s_ease-out_both] font-mono text-[11px] uppercase tracking-wider text-emerald-400">
                Gespeichert
              </span>
            )}
          </div>
        </form>
      </GlassCard>

      <GlassCard className="border-red-900/60 p-5">
        <h2 className="mb-1 font-mono text-xs uppercase tracking-wider text-red-400">Danger Zone</h2>
        <p className="mb-4 font-mono text-[11px] text-white/35">
          Diese Aktionen sind destruktiv und können nicht rückgängig gemacht werden.
        </p>
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between gap-4 border-t border-red-900/30 pt-3">
            <div>
              <div className="text-sm text-white">Alle Vorlagen löschen</div>
              <div className="font-mono text-[11px] text-white/35">Entfernt sämtliche gespeicherten Vorlagen.</div>
            </div>
            <button
              onClick={handleDeleteAllTemplates}
              disabled={busy === "__delete-templates__"}
              className="shrink-0 border border-red-900/60 px-3 py-1.5 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
            >
              Löschen
            </button>
          </div>
          <div className="flex items-center justify-between gap-4 border-t border-red-900/30 pt-3">
            <div>
              <div className="text-sm text-white">Cloud zurücksetzen</div>
              <div className="font-mono text-[11px] text-white/35">
                Stoppt alle Dienste und löscht alle Server- und Proxy-Daten.
              </div>
            </div>
            <button
              onClick={handleResetCloud}
              disabled={busy === "__reset-cloud__"}
              className="shrink-0 border border-red-900/60 px-3 py-1.5 font-mono text-[11px] uppercase tracking-wider text-red-400 transition-all duration-150 hover:bg-red-950/40 active:scale-95 disabled:opacity-50"
            >
              Zurücksetzen
            </button>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
