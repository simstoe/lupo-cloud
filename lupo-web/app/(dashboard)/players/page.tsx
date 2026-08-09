"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import * as api from "@/lib/api";
import type { PlayerInfo } from "@/lib/types";
import { GlassCard, Badge, LiveDot, CountUp, fadeStyle } from "@/components/ui";

export default function PlayersPage() {
  const { token } = useAuth();
  const [players, setPlayers] = useState<PlayerInfo[] | null>(null);

  const refresh = useCallback(() => {
    if (!token) return;
    api.listPlayers(token).then(setPlayers).catch(() => {});
  }, [token]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 3000);
    return () => clearInterval(interval);
  }, [refresh]);

  const grouped = new Map<string, PlayerInfo[]>();
  for (const player of players ?? []) {
    const list = grouped.get(player.serviceName) ?? [];
    list.push(player);
    grouped.set(player.serviceName, list);
  }

  return (
    <div className="flex flex-col gap-6">
      <GlassCard className="p-4">
        <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">Online</span>
        <div className="mt-1 flex items-center gap-2">
          <LiveDot color={(players?.length ?? 0) > 0 ? "bg-emerald-400" : "bg-white/30"} />
          <CountUp value={players?.length ?? 0} className="text-2xl font-semibold text-white" />
        </div>
      </GlassCard>

      <GlassCard>
        {players?.length === 0 && (
          <div className="px-4 py-8 text-center font-mono text-xs text-white/35">
            Aktuell sind keine Spieler online.
          </div>
        )}
        {[...grouped.entries()].map(([serviceName, servicePlayers], i) => (
          <div
            key={serviceName}
            style={fadeStyle(i)}
            className={`stagger-in px-4 py-3 ${i > 0 ? "border-t border-white/10" : ""}`}
          >
            <div className="mb-2 flex items-center gap-2">
              <span className="text-sm font-medium text-white">{serviceName}</span>
              <Badge>{servicePlayers.length}</Badge>
            </div>
            <div className="flex flex-wrap gap-2">
              {servicePlayers.map((p, j) => (
                <span
                  key={p.playerName}
                  style={fadeStyle(j, 25, 250)}
                  className="stagger-in border border-white/15 px-2.5 py-1 font-mono text-[11px] text-white/70 transition-colors duration-150 hover:border-white/30 hover:text-white"
                >
                  {p.playerName}
                </span>
              ))}
            </div>
          </div>
        ))}
      </GlassCard>
    </div>
  );
}
