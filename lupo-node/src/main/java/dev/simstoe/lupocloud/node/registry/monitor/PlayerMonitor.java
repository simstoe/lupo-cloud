package dev.simstoe.lupocloud.node.registry.monitor;

import dev.simstoe.lupocloud.api.models.PlayerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Periodically polls every running Paper service via {@link QueryClient} to build a
 * network-wide online-player snapshot, without requiring a companion plugin.
 */
public class PlayerMonitor {
    private static final long POLL_INTERVAL_SECONDS = 5;

    private final Supplier<Map<String, Integer>> runningPaperServicePorts;
    private final Executor queryExecutor;
    private final QueryClient queryClient = new QueryClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(PlayerMonitor::newDaemonThread);

    private volatile Map<String, QueryClient.QueryResult> snapshot = Map.of();

    public PlayerMonitor(Supplier<Map<String, Integer>> runningPaperServicePorts, Executor queryExecutor) {
        this.runningPaperServicePorts = runningPaperServicePorts;
        this.queryExecutor = queryExecutor;
        scheduler.scheduleWithFixedDelay(this::poll, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void poll() {
        Map<String, Integer> services = runningPaperServicePorts.get();
        Map<String, QueryClient.QueryResult> results = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = services.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(
                        () -> queryClient.query("127.0.0.1", entry.getValue())
                                .ifPresent(result -> results.put(entry.getKey(), result)),
                        queryExecutor))
                .toList();
        futures.forEach(CompletableFuture::join);

        snapshot = results;
    }

    public List<PlayerInfo> onlinePlayers() {
        return snapshot.entrySet().stream()
                .flatMap(entry -> entry.getValue().players().stream()
                        .map(playerName -> new PlayerInfo(playerName, entry.getKey())))
                .toList();
    }

    private static Thread newDaemonThread(Runnable r) {
        Thread thread = new Thread(r, "player-monitor");
        thread.setDaemon(true);
        return thread;
    }
}
