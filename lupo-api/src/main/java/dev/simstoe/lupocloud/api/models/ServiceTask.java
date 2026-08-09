package dev.simstoe.lupocloud.api.models;

import java.util.List;

public record ServiceTask(
        String name,
        ServiceType type,
        String templateName,
        int minMemoryMB,
        int maxMemoryMB,
        Integer cpuCoreLimit,
        int startPort,
        int minOnlineCount,
        int maxOnlineCount,
        boolean staticService,
        boolean autostart,
        String group,
        List<String> proxyGroups
) {
    public static ServiceTask create(String name, ServiceType type, int startPort) {
        int defaultMinMemory = type == ServiceType.PAPER ? 1024 : 512;
        int defaultMaxMemory = type == ServiceType.PAPER ? 2048 : 1024;
        return new ServiceTask(name, type, null, defaultMinMemory, defaultMaxMemory, null, startPort, 1, 1, false, false, null, null);
    }
}
