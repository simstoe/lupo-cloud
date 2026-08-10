package dev.simstoe.lupocloud.node.registry.monitor;

import dev.simstoe.lupocloud.api.models.HostMetrics;
import dev.simstoe.lupocloud.api.models.MonitoringSnapshot;
import dev.simstoe.lupocloud.api.models.ServiceMetrics;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MetricsCollector {
    private static final long POLL_INTERVAL_SECONDS = 5;

    public record TrackedProcess(String name, long pid, Instant startedAt) {}

    private final Supplier<List<TrackedProcess>> trackedProcesses;
    private final Executor sampleExecutor;
    private final ProcessMetrics processMetrics = new ProcessMetrics();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(MetricsCollector::newDaemonThread);

    private volatile MonitoringSnapshot snapshot = new MonitoringSnapshot(hostMetrics(), List.of());

    public MetricsCollector(Supplier<List<TrackedProcess>> trackedProcesses, Executor sampleExecutor) {
        this.trackedProcesses = trackedProcesses;
        this.sampleExecutor = sampleExecutor;
        scheduler.scheduleWithFixedDelay(this::poll, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void poll() {
        List<CompletableFuture<ServiceMetrics>> futures = trackedProcesses.get().stream()
                .map(process -> CompletableFuture.supplyAsync(() -> toServiceMetrics(process), sampleExecutor))
                .toList();
        List<ServiceMetrics> services = futures.stream().map(CompletableFuture::join).toList();
        snapshot = new MonitoringSnapshot(hostMetrics(), services);
    }

    private ServiceMetrics toServiceMetrics(TrackedProcess process) {
        long uptime = Duration.between(process.startedAt(), Instant.now()).getSeconds();
        return processMetrics.sample(process.pid())
                .map(stats -> new ServiceMetrics(process.name(), true, stats.cpuPercent(), stats.memoryMB(), uptime))
                .orElse(new ServiceMetrics(process.name(), true, 0, 0, uptime));
    }

    private static HostMetrics hostMetrics() {
        var osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        double cpuLoad = osBean.getCpuLoad();
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();

        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long usedDisk = totalDisk - root.getUsableSpace();

        return new HostMetrics(
                Math.max(0, cpuLoad) * 100,
                (totalMemory - freeMemory) / (1024 * 1024),
                totalMemory / (1024 * 1024),
                usedDisk / (1024 * 1024),
                totalDisk / (1024 * 1024)
        );
    }

    public MonitoringSnapshot snapshot() {
        return snapshot;
    }

    private static Thread newDaemonThread(Runnable r) {
        Thread thread = new Thread(r, "metrics-collector");
        thread.setDaemon(true);
        return thread;
    }
}
