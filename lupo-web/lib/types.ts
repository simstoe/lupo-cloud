export type ServiceType = "PAPER" | "VELOCITY";

export type CloudService = {
  name: string;
  type: ServiceType;
  port: number;
  uuid: string;
  running: boolean;
};

export type ServiceTask = {
  name: string;
  type: ServiceType;
  templateName: string | null;
  minMemoryMB: number;
  maxMemoryMB: number;
  cpuCoreLimit: number | null;
  startPort: number;
  minOnlineCount: number;
  maxOnlineCount: number;
  staticService: boolean;
  autostart: boolean;
  group: string | null;
  proxyGroups: string[] | null;
};

export type TemplateDto = {
  name: string;
  type: ServiceType;
};

export type NetworkSettings = {
  networkName: string;
  motd: string;
};

export type PlayerInfo = {
  playerName: string;
  serviceName: string;
};

export type ServiceMetrics = {
  name: string;
  running: boolean;
  cpuPercent: number;
  memoryMB: number;
  uptimeSeconds: number;
};

export type HostMetrics = {
  cpuPercent: number;
  usedMemoryMB: number;
  totalMemoryMB: number;
  usedDiskMB: number;
  totalDiskMB: number;
};

export type MonitoringSnapshot = {
  host: HostMetrics;
  services: ServiceMetrics[];
};
