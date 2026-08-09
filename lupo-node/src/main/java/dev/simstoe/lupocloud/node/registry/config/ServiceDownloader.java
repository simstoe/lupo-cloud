package dev.simstoe.lupocloud.node.registry.config;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.models.ServiceType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ServiceDownloader {
    private static final File CACHE_DIR = new File(".cache", "jars");

    private final PaperApiClient paperApiClient = new PaperApiClient();
    private final Map<ServiceType, CompletableFuture<File>> inFlightDownloads = new ConcurrentHashMap<>();

    public CompletableFuture<Boolean> provide(ServiceType type, File destination, ExecutorService executor) {
        return cachedJar(type, executor).handle((cachedJar, error) -> {
            if (error != null) {
                CloudLogger.error("Download of " + type + " failed: " + rootMessage(error));
                return false;
            }
            try {
                Files.copy(cachedJar.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                CloudLogger.error("Could not provide " + type + "-JAR: " + e.getMessage());
                return false;
            }
        });
    }

    private CompletableFuture<File> cachedJar(ServiceType type, ExecutorService executor) {
        return inFlightDownloads.computeIfAbsent(type, t ->
                CompletableFuture.supplyAsync(() -> downloadToCache(t), executor)
                        .whenComplete((file, error) -> inFlightDownloads.remove(t)));
    }

    private File downloadToCache(ServiceType type) {
        var resolved = resolveBuild(type);
        var cacheFile = new File(CACHE_DIR, resolved.fileName());

        if (cacheFile.exists()) {
            CloudLogger.info(type + "-JAR already in cache: " + resolved.fileName());
            return cacheFile;
        }

        if (!CACHE_DIR.exists()) CACHE_DIR.mkdirs();
        var tmpFile = new File(CACHE_DIR, resolved.fileName() + ".tmp");

        try {
            CloudLogger.info("Download " + type + "-JAR (" + resolved.fileName() + ")...");
            var connection = (HttpURLConnection) URI.create(resolved.url()).toURL().openConnection();

            try (var channel = Channels.newChannel(connection.getInputStream());
                 var fos = new FileOutputStream(tmpFile)) {
                fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            }

            Files.move(tmpFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CloudLogger.success(type + "-JAR downloaded.");
            return cacheFile;
        } catch (IOException e) {
            tmpFile.delete();
            throw new UncheckedIOException(e);
        }
    }

    private PaperApiClient.ResolvedBuild resolveBuild(ServiceType type) {
        try {
            return paperApiClient.resolveLatestBuild(type);
        } catch (Exception e) {
            CloudLogger.info("PaperMC-API not reachable (" + rootMessage(e) + "), usage of a fallback version " + type + ".");
            String url = type.getFallbackDownloadUrl();
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            return new PaperApiClient.ResolvedBuild(fileName, url);
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
