import type {
  CloudService,
  MonitoringSnapshot,
  NetworkSettings,
  PlayerInfo,
  ServiceTask,
  ServiceType,
  TemplateDto,
} from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export function wsUrl(path: string, token: string): string {
  const base = API_BASE_URL.replace(/^http/, "ws");
  return `${base}${path}?token=${encodeURIComponent(token)}`;
}

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(
  path: string,
  token: string | null,
  init?: RequestInit,
): Promise<T> {
  const headers: Record<string, string> = {
    ...(init?.body ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (!res.ok) {
    throw new ApiError(res.status, `${init?.method ?? "GET"} ${path} failed: ${res.status}`);
  }

  if (res.status === 204 || res.headers.get("Content-Length") === "0") {
    return undefined as T;
  }

  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export async function login(password: string): Promise<string> {
  const { token } = await request<{ token: string }>("/api/auth/login", null, {
    method: "POST",
    body: JSON.stringify({ password }),
  });
  return token;
}

export function listServices(token: string) {
  return request<CloudService[]>("/api/services", token);
}

export function getService(token: string, name: string) {
  return request<CloudService>(`/api/services/${encodeURIComponent(name)}`, token);
}

export function startService(
  token: string,
  body: { name: string; type: ServiceType; port: number; templateName: string | null },
) {
  return request<void>("/api/services", token, { method: "POST", body: JSON.stringify(body) });
}

export function stopService(token: string, name: string) {
  return request<void>(`/api/services/${encodeURIComponent(name)}/stop`, token, { method: "POST" });
}

export function stopAllServices(token: string) {
  return request<void>("/api/services/stop-all", token, { method: "POST" });
}

export function sendCommand(token: string, name: string, command: string) {
  return request<void>(`/api/services/${encodeURIComponent(name)}/command`, token, {
    method: "POST",
    body: JSON.stringify({ command }),
  });
}

export function getServiceLogs(token: string, name: string) {
  return request<string[]>(`/api/services/${encodeURIComponent(name)}/logs`, token);
}

export function listTasks(token: string) {
  return request<ServiceTask[]>("/api/tasks", token);
}

export function createTask(token: string, task: ServiceTask) {
  return request<void>("/api/tasks", token, { method: "POST", body: JSON.stringify(task) });
}

export function updateTask(token: string, name: string, task: ServiceTask) {
  return request<void>(`/api/tasks/${encodeURIComponent(name)}`, token, {
    method: "PUT",
    body: JSON.stringify(task),
  });
}

export function deleteTask(token: string, name: string) {
  return request<void>(`/api/tasks/${encodeURIComponent(name)}`, token, { method: "DELETE" });
}

export function startTask(token: string, name: string) {
  return request<void>(`/api/tasks/${encodeURIComponent(name)}/start`, token, { method: "POST" });
}

export function listTemplates(token: string) {
  return request<TemplateDto[]>("/api/templates", token);
}

export function saveTemplate(token: string, templateName: string, sourceService: string) {
  return request<void>("/api/templates", token, {
    method: "POST",
    body: JSON.stringify({ templateName, sourceService }),
  });
}

export function deleteTemplate(token: string, name: string) {
  return request<void>(`/api/templates/${encodeURIComponent(name)}`, token, { method: "DELETE" });
}

export function listBackups(token: string, serviceName: string) {
  return request<string[]>(`/api/services/${encodeURIComponent(serviceName)}/backups`, token);
}

export function createBackup(token: string, serviceName: string) {
  return request<void>(`/api/services/${encodeURIComponent(serviceName)}/backups`, token, {
    method: "POST",
  });
}

export function restoreBackup(token: string, serviceName: string, file: string) {
  return request<void>(
    `/api/services/${encodeURIComponent(serviceName)}/backups/${encodeURIComponent(file)}/restore`,
    token,
    { method: "POST" },
  );
}

export function deleteBackup(token: string, serviceName: string, file: string) {
  return request<void>(
    `/api/services/${encodeURIComponent(serviceName)}/backups/${encodeURIComponent(file)}`,
    token,
    { method: "DELETE" },
  );
}

export function getSettings(token: string) {
  return request<NetworkSettings>("/api/settings", token);
}

export function updateSettings(token: string, settings: NetworkSettings) {
  return request<void>("/api/settings", token, { method: "PUT", body: JSON.stringify(settings) });
}

export function deleteAllTemplates(token: string) {
  return request<void>("/api/settings/danger/delete-all-templates", token, { method: "POST" });
}

export function resetCloud(token: string) {
  return request<void>("/api/settings/danger/reset-cloud", token, { method: "POST" });
}

export function listPlayers(token: string) {
  return request<PlayerInfo[]>("/api/players", token);
}

export function getMonitoring(token: string) {
  return request<MonitoringSnapshot>("/api/monitoring", token);
}

export function getSetupStatus(token: string) {
  return request<{ needsSetup: boolean }>("/api/setup/status", token);
}

export function completeSetup(token: string) {
  return request<void>("/api/setup/complete", token, { method: "POST" });
}
