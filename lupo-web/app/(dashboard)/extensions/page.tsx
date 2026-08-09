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


  return (
    <h1>Test</h1>
  );
}
