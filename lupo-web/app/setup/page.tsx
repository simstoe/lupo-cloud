"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth, useRequireAuth } from "@/lib/auth";
import {
  completeSetup,
  createAdminAccount,
  createTask,
  getMemoryBudget,
  getSettings,
  getSetupStatus,
  startTask,
  updateSettings,
} from "@/lib/api";
import type { ServiceTask } from "@/lib/types";
import MemoryBudgetPicker, { formatMB, presetValueMB } from "@/components/MemoryBudgetPicker";
import { IconServer, IconGrid, IconCheck, IconUsers, IconMemory } from "@/components/icons";

type StepKey = "admin" | "memory" | "proxy" | "lobby" | "done";

const STEP_LABELS: Record<StepKey, string> = {
  admin: "Admin",
  memory: "RAM",
  proxy: "Proxy",
  lobby: "Lobby",
  done: "Fertig",
};

function proxyTask(name: string, port: number, minMemoryMB: number, maxMemoryMB: number): ServiceTask {
  return {
    name,
    type: "VELOCITY",
    templateName: null,
    minMemoryMB,
    maxMemoryMB,
    cpuCoreLimit: null,
    startPort: port,
    minOnlineCount: 1,
    maxOnlineCount: 1,
    staticService: true,
    autostart: true,
    group: null,
    proxyGroups: null,
  };
}

function lobbyTask(name: string, port: number, minMemoryMB: number, maxMemoryMB: number): ServiceTask {
  return {
    name,
    type: "PAPER",
    templateName: null,
    minMemoryMB,
    maxMemoryMB,
    cpuCoreLimit: null,
    startPort: port,
    minOnlineCount: 1,
    maxOnlineCount: 1,
    staticService: true,
    autostart: true,
    group: null,
    proxyGroups: null,
  };
}

export default function SetupPage() {
  const token = useRequireAuth();
  const { logout } = useAuth();
  const router = useRouter();

  const [steps, setSteps] = useState<StepKey[] | null>(null);
  const [step, setStep] = useState(0);

  const [adminUsername, setAdminUsername] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [adminPasswordConfirm, setAdminPasswordConfirm] = useState("");

  const [hostTotalMB, setHostTotalMB] = useState<number | null>(null);
  const [budgetMB, setBudgetMB] = useState<number | null>(null);

  const [createProxy, setCreateProxy] = useState(true);
  const [proxyName, setProxyName] = useState("proxy");
  const [proxyPort, setProxyPort] = useState(25565);
  const [proxyMinMemory, setProxyMinMemory] = useState(512);
  const [proxyMaxMemory, setProxyMaxMemory] = useState(1024);

  const [createLobby, setCreateLobby] = useState(true);
  const [lobbyName, setLobbyName] = useState("lobby");
  const [lobbyPort, setLobbyPort] = useState(25580);
  const [lobbyMinMemory, setLobbyMinMemory] = useState(1024);
  const [lobbyMaxMemory, setLobbyMaxMemory] = useState(2048);

  const [startNow, setStartNow] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    Promise.all([getSetupStatus(token), getMemoryBudget(token)])
      .then(([status, budget]) => {
        setSteps(
          status.needsAdminAccount
            ? ["admin", "memory", "proxy", "lobby", "done"]
            : ["memory", "proxy", "lobby", "done"],
        );
        setHostTotalMB(budget.hostTotalMB);
        setBudgetMB(budget.limitMB ?? presetValueMB("recommended", budget.hostTotalMB));
      })
      .catch(() => setError("Setup-Status konnte nicht geladen werden."));
  }, [token]);

  if (!token || steps === null || hostTotalMB === null) return null;

  const current = steps[step];
  const plannedMemoryMB = (createProxy ? proxyMaxMemory : 0) + (createLobby ? lobbyMaxMemory : 0);
  const overBudget = budgetMB !== null && plannedMemoryMB > budgetMB;

  function goNext() {
    setError(null);
    setStep((s) => Math.min(s + 1, steps!.length - 1));
  }

  function goBack() {
    setError(null);
    setStep((s) => Math.max(s - 1, 0));
  }

  async function handleCreateAdmin(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (adminUsername.trim().length === 0 || adminPassword.length === 0) {
      setError("Benutzername und Passwort dürfen nicht leer sein.");
      return;
    }
    if (adminPassword !== adminPasswordConfirm) {
      setError("Passwörter stimmen nicht überein.");
      return;
    }

    setBusy(true);
    try {
      await createAdminAccount(token!, adminUsername.trim(), adminPassword);
      goNext();
    } catch {
      setError("Admin-Account konnte nicht angelegt werden.");
    } finally {
      setBusy(false);
    }
  }

  async function handleFinish() {
    setError(null);
    setBusy(true);
    try {
      const settings = await getSettings(token!);
      await updateSettings(token!, { ...settings, maxMemoryMB: budgetMB });

      if (createLobby) await createTask(token!, lobbyTask(lobbyName, lobbyPort, lobbyMinMemory, lobbyMaxMemory));
      if (createProxy) await createTask(token!, proxyTask(proxyName, proxyPort, proxyMinMemory, proxyMaxMemory));

      if (startNow) {
        if (createLobby) await startTask(token!, lobbyName);
        if (createProxy) await startTask(token!, proxyName);
      }

      await completeSetup(token!);
      router.replace("/");
    } catch {
      setError("Einrichtung fehlgeschlagen. Bitte Eingaben prüfen und erneut versuchen.");
      setBusy(false);
    }
  }

  const inputClass =
    "border border-white/15 bg-white/[0.02] px-3 py-2 text-sm text-white outline-none transition-colors focus:border-white/40 disabled:opacity-50";
  const labelClass = "font-mono text-[10px] uppercase tracking-wider text-white/40";
  const sectionClass = "flex items-center gap-2 font-mono text-[11px] uppercase tracking-wider text-white/50";
  const primaryButtonClass =
    "flex items-center justify-center gap-2 border border-white bg-white px-5 py-2.5 font-mono text-xs font-semibold uppercase tracking-wider text-black transition-all duration-150 hover:opacity-90 active:scale-[0.98] disabled:opacity-60";
  const ghostButtonClass =
    "border border-white/15 px-4 py-2.5 font-mono text-xs uppercase tracking-wider text-white/50 transition-colors hover:border-white/30 hover:text-white/80 disabled:opacity-60";

  return (
    <div className="bg-grid flex h-full items-center justify-center p-6">
      <div className="w-full max-w-lg animate-[fadeInUp_0.5s_ease-out_both] border border-white/15 bg-[#0a0a0a] p-8">
        <div className="flex items-start justify-between">
          <div>
            <span className="font-mono text-[11px] uppercase tracking-wider text-white/40">
              Erste Einrichtung · Schritt {step + 1} von {steps.length}
            </span>
            <h1 className="font-minecraft text-3xl uppercase tracking-wide text-white">
              {current === "admin" && "Admin-Account"}
              {current === "memory" && "RAM-Budget"}
              {current === "proxy" && "Proxy"}
              {current === "lobby" && "Lobby"}
              {current === "done" && "Zusammenfassung"}
            </h1>
          </div>
          {current !== "admin" && (
            <button
              type="button"
              onClick={logout}
              className="font-mono text-[10px] uppercase tracking-wider text-white/30 hover:text-white/60"
            >
              Abmelden
            </button>
          )}
        </div>

        <div className="mt-5 flex items-center gap-1.5">
          {steps.map((key, index) => (
            <div key={key} className="flex flex-1 flex-col gap-1.5">
              <div className={`h-0.5 w-full ${index <= step ? "bg-white" : "bg-white/15"}`} />
              <span
                className={`font-mono text-[9px] uppercase tracking-wider ${
                  index === step ? "text-white" : "text-white/30"
                }`}
              >
                {STEP_LABELS[key]}
              </span>
            </div>
          ))}
        </div>

        {current === "admin" && (
          <form onSubmit={handleCreateAdmin} className="mt-6 flex flex-col gap-5">
            <p className="font-mono text-[11px] leading-relaxed text-white/40">
              Du wurdest automatisch angemeldet, weil noch kein Admin-Account existiert. Lege jetzt Benutzername und
              Passwort fest — damit meldest du dich künftig im Dashboard an.
            </p>

            <div className={sectionClass}>
              <IconUsers className="h-3.5 w-3.5" /> Admin-Account
            </div>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Benutzername</span>
              <input
                value={adminUsername}
                onChange={(e) => setAdminUsername(e.target.value)}
                disabled={busy}
                autoFocus
                autoComplete="username"
                className={inputClass}
              />
            </label>

            <div className="grid grid-cols-2 gap-3">
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Passwort</span>
                <input
                  type="password"
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  disabled={busy}
                  autoComplete="new-password"
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Bestätigen</span>
                <input
                  type="password"
                  value={adminPasswordConfirm}
                  onChange={(e) => setAdminPasswordConfirm(e.target.value)}
                  disabled={busy}
                  autoComplete="new-password"
                  className={inputClass}
                />
              </label>
            </div>

            {error && <div className="font-mono text-[11px] text-red-400">{error}</div>}

            <button type="submit" disabled={busy} className={`${primaryButtonClass} mt-1`}>
              {busy && <span className="h-3 w-3 animate-spin border-2 border-black/30 border-t-black" />}
              Account anlegen &amp; weiter
            </button>
          </form>
        )}

        {current === "memory" && (
          <div className="mt-6 flex flex-col gap-5">
            <p className="font-mono text-[11px] leading-relaxed text-white/40">
              Lege fest, wie viel Arbeitsspeicher die Cloud insgesamt an Services vergeben darf. Services, die das
              Limit überschreiten würden, werden nicht gestartet. Später jederzeit in den Einstellungen änderbar.
            </p>

            <div className={sectionClass}>
              <IconMemory className="h-3.5 w-3.5" /> RAM-Budget
            </div>

            <MemoryBudgetPicker hostTotalMB={hostTotalMB} value={budgetMB} onChange={setBudgetMB} disabled={busy} />

            {error && <div className="font-mono text-[11px] text-red-400">{error}</div>}

            <div className="mt-1 flex gap-3">
              {step > 0 && (
                <button type="button" onClick={goBack} disabled={busy} className={ghostButtonClass}>
                  Zurück
                </button>
              )}
              <button type="button" onClick={goNext} disabled={busy} className={`${primaryButtonClass} flex-1`}>
                Weiter
              </button>
            </div>
          </div>
        )}

        {current === "proxy" && (
          <div className="mt-6 flex flex-col gap-5">
            <p className="font-mono text-[11px] leading-relaxed text-white/40">
              Der Proxy ist der Einstiegspunkt für Spieler — auf diesen Port verbinden sie sich.
            </p>

            <div className={sectionClass}>
              <IconGrid className="h-3.5 w-3.5" /> Proxy
            </div>

            <label className="flex items-center gap-2 font-mono text-[11px] text-white/50">
              <input
                type="checkbox"
                checked={createProxy}
                onChange={(e) => setCreateProxy(e.target.checked)}
                disabled={busy}
                className="h-3.5 w-3.5 accent-white"
              />
              Proxy jetzt anlegen
            </label>

            <div className="grid grid-cols-2 gap-3">
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Name</span>
                <input
                  value={proxyName}
                  onChange={(e) => setProxyName(e.target.value)}
                  disabled={busy || !createProxy}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Port</span>
                <input
                  type="number"
                  value={proxyPort}
                  onChange={(e) => setProxyPort(Number(e.target.value))}
                  disabled={busy || !createProxy}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>RAM min (MB)</span>
                <input
                  type="number"
                  value={proxyMinMemory}
                  onChange={(e) => setProxyMinMemory(Number(e.target.value))}
                  disabled={busy || !createProxy}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>RAM max (MB)</span>
                <input
                  type="number"
                  value={proxyMaxMemory}
                  onChange={(e) => setProxyMaxMemory(Number(e.target.value))}
                  disabled={busy || !createProxy}
                  className={inputClass}
                />
              </label>
            </div>

            {error && <div className="font-mono text-[11px] text-red-400">{error}</div>}

            <div className="mt-1 flex gap-3">
              <button type="button" onClick={goBack} disabled={busy} className={ghostButtonClass}>
                Zurück
              </button>
              <button type="button" onClick={goNext} disabled={busy} className={`${primaryButtonClass} flex-1`}>
                Weiter
              </button>
            </div>
          </div>
        )}

        {current === "lobby" && (
          <div className="mt-6 flex flex-col gap-5">
            <p className="font-mono text-[11px] leading-relaxed text-white/40">
              Die Lobby ist der erste Server, auf den der Proxy Spieler weiterleitet.
            </p>

            <div className={sectionClass}>
              <IconServer className="h-3.5 w-3.5" /> Lobby
            </div>

            <label className="flex items-center gap-2 font-mono text-[11px] text-white/50">
              <input
                type="checkbox"
                checked={createLobby}
                onChange={(e) => setCreateLobby(e.target.checked)}
                disabled={busy}
                className="h-3.5 w-3.5 accent-white"
              />
              Lobby jetzt anlegen
            </label>

            <div className="grid grid-cols-2 gap-3">
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Name</span>
                <input
                  value={lobbyName}
                  onChange={(e) => setLobbyName(e.target.value)}
                  disabled={busy || !createLobby}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>Port</span>
                <input
                  type="number"
                  value={lobbyPort}
                  onChange={(e) => setLobbyPort(Number(e.target.value))}
                  disabled={busy || !createLobby}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>RAM min (MB)</span>
                <input
                  type="number"
                  value={lobbyMinMemory}
                  onChange={(e) => setLobbyMinMemory(Number(e.target.value))}
                  disabled={busy || !createLobby}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className={labelClass}>RAM max (MB)</span>
                <input
                  type="number"
                  value={lobbyMaxMemory}
                  onChange={(e) => setLobbyMaxMemory(Number(e.target.value))}
                  disabled={busy || !createLobby}
                  className={inputClass}
                />
              </label>
            </div>

            {error && <div className="font-mono text-[11px] text-red-400">{error}</div>}

            <div className="mt-1 flex gap-3">
              <button type="button" onClick={goBack} disabled={busy} className={ghostButtonClass}>
                Zurück
              </button>
              <button type="button" onClick={goNext} disabled={busy} className={`${primaryButtonClass} flex-1`}>
                Weiter
              </button>
            </div>
          </div>
        )}

        {current === "done" && (
          <div className="mt-6 flex flex-col gap-5">
            <p className="font-mono text-[11px] leading-relaxed text-white/40">
              Alles bereit — prüfe die Zusammenfassung und schließe die Einrichtung ab.
            </p>

            <dl className="flex flex-col gap-2 border border-white/10 bg-white/[0.02] p-3">
              <div className="flex items-baseline justify-between gap-3">
                <dt className={labelClass}>RAM-Budget</dt>
                <dd className="font-mono text-[11px] text-white">{formatMB(budgetMB)}</dd>
              </div>
              <div className="flex items-baseline justify-between gap-3">
                <dt className={labelClass}>Proxy</dt>
                <dd className="font-mono text-[11px] text-white">
                  {createProxy ? `${proxyName} · Port ${proxyPort} · ${proxyMaxMemory} MB` : "wird übersprungen"}
                </dd>
              </div>
              <div className="flex items-baseline justify-between gap-3">
                <dt className={labelClass}>Lobby</dt>
                <dd className="font-mono text-[11px] text-white">
                  {createLobby ? `${lobbyName} · Port ${lobbyPort} · ${lobbyMaxMemory} MB` : "wird übersprungen"}
                </dd>
              </div>
              <div className="flex items-baseline justify-between gap-3 border-t border-white/10 pt-2">
                <dt className={labelClass}>Belegt vom Budget</dt>
                <dd className={`font-mono text-[11px] ${overBudget ? "text-red-400" : "text-white"}`}>
                  {plannedMemoryMB} MB{budgetMB !== null && ` von ${budgetMB} MB`}
                </dd>
              </div>
            </dl>

            {overBudget && (
              <div className="font-mono text-[11px] leading-relaxed text-red-400">
                Proxy und Lobby brauchen zusammen mehr RAM als das Budget zulässt — sie würden nicht starten. Gehe
                zurück und erhöhe das Budget oder reduziere den RAM der Services.
              </div>
            )}

            <label className="flex items-center gap-2 font-mono text-[11px] text-white/50">
              <input
                type="checkbox"
                checked={startNow}
                onChange={(e) => setStartNow(e.target.checked)}
                disabled={busy}
                className="h-3.5 w-3.5 accent-white"
              />
              Services direkt nach der Einrichtung starten
            </label>

            {error && <div className="font-mono text-[11px] text-red-400">{error}</div>}

            <div className="mt-1 flex gap-3">
              <button type="button" onClick={goBack} disabled={busy} className={ghostButtonClass}>
                Zurück
              </button>
              <button
                type="button"
                onClick={handleFinish}
                disabled={busy || overBudget}
                className={`${primaryButtonClass} flex-1`}
              >
                {busy ? (
                  <span className="h-3 w-3 animate-spin border-2 border-black/30 border-t-black" />
                ) : (
                  <IconCheck className="h-3.5 w-3.5" />
                )}
                Einrichtung abschließen
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
