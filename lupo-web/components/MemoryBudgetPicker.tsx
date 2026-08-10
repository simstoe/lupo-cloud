"use client";

import { useMemo, useState } from "react";

export type MemoryPresetKey = "recommended" | "conservative" | "maximum" | "unlimited" | "custom";

function roundDown512(mb: number): number {
  return Math.max(512, Math.floor(mb / 512) * 512);
}

export function presetValueMB(preset: MemoryPresetKey, hostTotalMB: number): number | null {
  switch (preset) {
    case "recommended":
      return roundDown512(Math.max(1024, hostTotalMB - 2048));
    case "conservative":
      return roundDown512(hostTotalMB * 0.5);
    case "maximum":
      return roundDown512(hostTotalMB * 0.9);
    case "unlimited":
      return null;
    case "custom":
      return null;
  }
}

export function formatMB(mb: number | null): string {
  if (mb === null) return "Kein Limit";
  if (mb < 1024) return `${mb} MB`;
  const gb = mb / 1024;
  return `${Number.isInteger(gb) ? gb : gb.toFixed(1)} GB`;
}

const PRESETS: { key: MemoryPresetKey; label: string; hint: string }[] = [
  { key: "recommended", label: "Empfohlen", hint: "Host-RAM minus 2 GB Reserve fürs System" },
  { key: "conservative", label: "Konservativ", hint: "Die Hälfte des Host-RAMs" },
  { key: "maximum", label: "Maximum", hint: "90 % — wenig Reserve, nur für dedizierte Server" },
  { key: "custom", label: "Eigener Wert", hint: "Limit selbst festlegen" },
  { key: "unlimited", label: "Kein Limit", hint: "Cloud darf unbegrenzt Services starten" },
];

export default function MemoryBudgetPicker({
  hostTotalMB,
  value,
  onChange,
  disabled = false,
}: {
  hostTotalMB: number;
  value: number | null;
  onChange: (mb: number | null) => void;
  disabled?: boolean;
}) {
  const [selected, setSelected] = useState<MemoryPresetKey>(() => {
    if (value === null) return "unlimited";
    const match = PRESETS.find((p) => p.key !== "custom" && presetValueMB(p.key, hostTotalMB) === value);
    return match ? match.key : "custom";
  });
  const [customGB, setCustomGB] = useState(() => (value === null ? "4" : (value / 1024).toString()));

  const hostLabel = useMemo(() => formatMB(hostTotalMB), [hostTotalMB]);

  function selectPreset(key: MemoryPresetKey) {
    setSelected(key);
    if (key === "custom") {
      const gb = Number(customGB);
      onChange(Number.isFinite(gb) && gb > 0 ? Math.round(gb * 1024) : null);
    } else {
      onChange(presetValueMB(key, hostTotalMB));
    }
  }

  function changeCustom(raw: string) {
    setCustomGB(raw);
    const gb = Number(raw);
    onChange(Number.isFinite(gb) && gb > 0 ? Math.round(gb * 1024) : null);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between border border-white/10 bg-white/[0.02] px-3 py-2">
        <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">RAM im Server</span>
        <span className="font-mono text-[11px] text-white">{hostLabel}</span>
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {PRESETS.map((preset) => {
          const presetMB = preset.key === "custom" ? null : presetValueMB(preset.key, hostTotalMB);
          const isSelected = selected === preset.key;
          return (
            <button
              key={preset.key}
              type="button"
              disabled={disabled}
              onClick={() => selectPreset(preset.key)}
              className={`flex flex-col items-start gap-1 border px-3 py-2.5 text-left transition-colors disabled:opacity-50 ${
                isSelected
                  ? "border-white bg-white/10"
                  : "border-white/15 bg-white/[0.02] hover:border-white/30"
              }`}
            >
              <div className="flex w-full items-baseline justify-between gap-2">
                <span className="font-mono text-[11px] uppercase tracking-wider text-white">{preset.label}</span>
                {preset.key !== "custom" && (
                  <span className="font-mono text-[11px] text-white/50">{formatMB(presetMB)}</span>
                )}
              </div>
              <span className="font-mono text-[10px] leading-relaxed text-white/35">{preset.hint}</span>
            </button>
          );
        })}
      </div>

      {selected === "custom" && (
        <label className="flex flex-col gap-1.5">
          <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Limit in GB</span>
          <input
            type="number"
            min={1}
            step={0.5}
            value={customGB}
            onChange={(e) => changeCustom(e.target.value)}
            disabled={disabled}
            className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
          />
        </label>
      )}

      {value !== null && value > hostTotalMB && (
        <div className="font-mono text-[11px] text-amber-400">
          Achtung: Das Limit liegt über dem tatsächlich vorhandenen RAM ({hostLabel}).
        </div>
      )}
    </div>
  );
}
