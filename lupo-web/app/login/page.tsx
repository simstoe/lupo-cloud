"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { IconBolt } from "@/components/icons";
import { LiveDot } from "@/components/ui";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username, password);
      router.replace("/");
    } catch {
      setError("Ungültiger Benutzername oder Passwort.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="bg-grid flex h-full items-center justify-center p-6">
      <div className="w-full max-w-sm animate-[fadeInUp_0.5s_ease-out_both] border border-white/15 bg-[#0a0a0a] p-8">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center border border-white/20 transition-transform duration-300 hover:rotate-12 hover:scale-110">
            <IconBolt className="h-4 w-4 text-white" />
          </div>
          <span className="font-minecraft text-4xl uppercase tracking-wide text-white">LUPO CLOUD</span>
        </div>

        <div className="mt-6 flex items-center justify-between border border-white/10 bg-white/[0.02] px-3 py-2">
          <span className="font-mono text-[11px] text-white/40">
            {process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"}
          </span>
          <div className="flex items-center gap-1.5">
            <LiveDot color="bg-white" />
            <span className="font-mono text-[10px] uppercase tracking-wider text-white/40">Erreichbar</span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
          <label className="flex flex-col gap-1.5">
            <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Benutzername</span>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              type="text"
              autoFocus
              disabled={loading}
              autoComplete="username"
              className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">Passwort</span>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              disabled={loading}
              autoComplete="current-password"
              placeholder="••••••••"
              className="border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50"
            />
          </label>

          {error && (
            <div className="animate-[fadeInUp_0.25s_ease-out_both] font-mono text-[11px] text-red-400">{error}</div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-2 flex items-center justify-center gap-2 border border-white bg-white py-2.5 font-mono text-xs font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-[0.98] disabled:opacity-60"
          >
            {loading && <span className="h-3 w-3 animate-spin border-2 border-black/30 border-t-black" />}
            {loading ? "Verbinde…" : "Anmelden"}
          </button>
        </form>

        <div className="mt-5 border-t border-white/10 pt-4 text-center font-mono text-[10px] uppercase tracking-wider text-white/25">
          lupo.cloud
        </div>
      </div>
    </div>
  );
}
