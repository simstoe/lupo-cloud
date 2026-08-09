package dev.simstoe.lupocloud.node.registry.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

/** Samples CPU/RAM usage of a child process via {@code ps}, since a JVM cannot read another process's RSS directly. */
public class ProcessMetrics {
    public record ProcessStats(double cpuPercent, long memoryMB) {}

    public Optional<ProcessStats> sample(long pid) {
        try {
            Process ps = new ProcessBuilder("ps", "-o", "%cpu=,rss=", "-p", String.valueOf(pid))
                    .redirectErrorStream(true)
                    .start();

            String line;
            try (var reader = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
                line = reader.readLine();
            }
            ps.waitFor();
            if (line == null || line.isBlank()) return Optional.empty();

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) return Optional.empty();

            double cpuPercent = Double.parseDouble(parts[0]);
            long memoryMB = Long.parseLong(parts[1]) / 1024;
            return Optional.of(new ProcessStats(cpuPercent, memoryMB));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
