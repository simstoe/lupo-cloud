package dev.simstoe.lupocloud.api.models;

public record HostMetrics(double cpuPercent, long usedMemoryMB, long totalMemoryMB, long usedDiskMB, long totalDiskMB) {}
