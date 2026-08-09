package dev.simstoe.lupocloud.api.models;

public record ServiceMetrics(String name, boolean running, double cpuPercent, long memoryMB, long uptimeSeconds) {}
