"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import {Button} from "@/components/ui/button";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(password);
      router.replace("/");
    } catch {
      setError("Invalid password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
      <div className="flex min-h-[80vh] flex-1 items-center justify-center px-4">
        <form
            onSubmit={handleSubmit}
            className="w-full max-w-sm rounded-2xl border border-neutral-800 bg-neutral-900/80 p-8 shadow-2xl backdrop-blur-xl transition-all"
        >
          <div className="mb-6 text-center">
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl border border-neutral-700/50 bg-neutral-800/50 text-neutral-200 shadow-inner">
              <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="h-5 w-5"
              >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"
                />
              </svg>
            </div>
            <h1 className="text-xl font-bold tracking-tight text-white">LUPO Dashboard</h1>
            <p className="mt-1 text-xs text-neutral-400">Bitte gib dein Admin-Passwort ein</p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-neutral-400">
                Admin-Passwort
              </label>
              <input
                  type="password"
                  autoFocus
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full rounded-lg border border-neutral-800 bg-neutral-950/60 px-3.5 py-2.5 text-sm text-white placeholder-neutral-600 outline-none transition-all focus:border-neutral-600 focus:ring-2 focus:ring-neutral-700/50"
              />
            </div>

            {error && (
                <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-400">
                  {error}
                </div>
            )}

            <Button
                type="submit"
                disabled={submitting}
                variant="outline"
                className="w-full rounded-lg border-neutral-700 bg-neutral-800 py-2.5 font-medium text-white transition-all hover:bg-neutral-700 hover:border-neutral-600 active:scale-[0.98] disabled:opacity-50"
            >
              {submitting ? (
                  <span className="flex items-center justify-center gap-2">
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Wird angemeldet...
          </span>
              ) : (
                  "Sign in"
              )}
            </Button>
          </div>
        </form>
      </div>
  );
}
