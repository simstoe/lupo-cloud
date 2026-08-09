package dev.simstoe.lupocloud.api.models;

import java.util.List;

public record MonitoringSnapshot(HostMetrics host, List<ServiceMetrics> services) {}
