package dev.simstoe.lupocloud.api.models;

public record MemoryBudget(Integer limitMB, int committedMB, long hostTotalMB) {}
