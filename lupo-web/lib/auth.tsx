"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { login as apiLogin, bootstrapLogin } from "./api";

const STORAGE_KEY = "lupo_token";

type AuthContextValue = {
  token: string | null;
  ready: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      setToken(stored);
      setReady(true);
      return;
    }

    // No stored session: while no admin account exists yet, the backend hands out a token to
    // anyone reaching the dashboard so the very first visitor lands straight in /setup.
    bootstrapLogin().then((bootstrapToken) => {
      if (bootstrapToken) {
        localStorage.setItem(STORAGE_KEY, bootstrapToken);
        setToken(bootstrapToken);
      }
      setReady(true);
    });
  }, []);

  async function login(username: string, password: string) {
    const newToken = await apiLogin(username, password);
    localStorage.setItem(STORAGE_KEY, newToken);
    setToken(newToken);
  }

  function logout() {
    localStorage.removeItem(STORAGE_KEY);
    setToken(null);
  }

  return <AuthContext.Provider value={{ token, ready, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export function useRequireAuth(): string | null {
  const { token, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && !token) router.replace("/login");
  }, [ready, token, router]);

  return token;
}
