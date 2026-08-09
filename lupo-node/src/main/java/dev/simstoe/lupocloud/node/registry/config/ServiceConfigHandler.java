package dev.simstoe.lupocloud.node.registry.config;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import dev.simstoe.lupocloud.node.registry.task.TaskManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServiceConfigHandler {
    private static final File CANONICAL_SECRET_FILE = new File("local", "forwarding.secret");
    private static final String TASK_MARKER_FILE = ".cloud-task";

    private final TaskManager taskManager;
    private final SettingsManager settingsManager;

    public ServiceConfigHandler(TaskManager taskManager, SettingsManager settingsManager) {
        this.taskManager = taskManager;
        this.settingsManager = settingsManager;
    }

    public void writeTaskMarker(File serviceDir, String taskName) {
        try {
            Files.writeString(new File(serviceDir, TASK_MARKER_FILE).toPath(), taskName);
        } catch (IOException ignored) {}
    }

    private Optional<ServiceTask> resolveTask(File serviceDir) {
        File marker = new File(serviceDir, TASK_MARKER_FILE);
        if (!marker.isFile()) return Optional.empty();
        try {
            String taskName = Files.readString(marker.toPath()).trim();
            return taskName.isEmpty() ? Optional.empty() : taskManager.getTask(taskName);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public boolean shouldAutostart(File serviceDir) {
        return resolveTask(serviceDir).map(ServiceTask::autostart).orElse(true);
    }

    public Set<String> resolveProxyGroups(File proxyDir) {
        List<String> groups = resolveTask(proxyDir).map(ServiceTask::proxyGroups).orElse(null);
        return (groups == null || groups.isEmpty()) ? Set.of() : new HashSet<>(groups);
    }

    private boolean isVisibleToProxy(File serverDir, Set<String> allowedGroups) {
        if (allowedGroups.isEmpty()) return true;
        String group = resolveTask(serverDir).map(ServiceTask::group).orElse(null);
        return group == null || allowedGroups.contains(group);
    }

    public void acceptEula(File serverDir) {
        try (var writer = new FileWriter(new File(serverDir, "eula.txt"))) {
            writer.write("eula=true\n");
        } catch (IOException ignored) {}
    }

    public void updateServerPort(File serverDir, int port) {
        var propsFile = new File(serverDir, "server.properties");

        try {
            List<String> lines = propsFile.exists() ? Files.readAllLines(propsFile.toPath()) : new ArrayList<>();
            boolean updated = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("server-port=")) {
                    lines.set(i, "server-port=" + port);
                    updated = true;
                }
            }
            if (!updated) lines.add("server-port=" + port);
            Files.write(propsFile.toPath(), lines);
        } catch (IOException ignored) {}
    }

    public void enableQuery(File serverDir, int port) {
        var propsFile = new File(serverDir, "server.properties");

        try {
            List<String> lines = propsFile.exists() ? Files.readAllLines(propsFile.toPath()) : new ArrayList<>();
            boolean queryUpdated = false;
            boolean portUpdated = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("enable-query=")) {
                    lines.set(i, "enable-query=true");
                    queryUpdated = true;
                } else if (lines.get(i).startsWith("query.port=")) {
                    lines.set(i, "query.port=" + port);
                    portUpdated = true;
                }
            }
            if (!queryUpdated) lines.add("enable-query=true");
            if (!portUpdated) lines.add("query.port=" + port);
            Files.write(propsFile.toPath(), lines);
        } catch (IOException ignored) {}
    }

    public void configurePaperForVelocity(File serverDir) {
        var propsFile = new File(serverDir, "server.properties");

        try {
            List<String> lines = propsFile.exists() ? Files.readAllLines(propsFile.toPath()) : new ArrayList<>();
            boolean updated = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("online-mode=")) {
                    lines.set(i, "online-mode=false");
                    updated = true;
                }
            }
            if (!updated) lines.add("online-mode=false");
            Files.write(propsFile.toPath(), lines);
        } catch (IOException ignored) {}

        var configDir = new File(serverDir, "config");
        if (!configDir.exists()) configDir.mkdirs();
        var paperGlobal = new File(configDir, "paper-global.yml");

        String secret;
        try {
            secret = canonicalSecret();
        } catch (IOException e) {
            CloudLogger.error("Could not read/generate the forwarding secret: " + e.getMessage());
            return;
        }

        try {
            String proxyConfig = "\nproxies:\n  velocity:\n    enabled: true\n    secret: \"" + secret + "\"\n";
            if (!paperGlobal.exists()) {
                Files.writeString(paperGlobal.toPath(), proxyConfig);
            } else {
                String content = Files.readString(paperGlobal.toPath());
                if (!content.contains("velocity:")) {
                    Files.writeString(paperGlobal.toPath(), proxyConfig, StandardOpenOption.APPEND);
                } else if (!content.contains("secret: \"" + secret + "\"")) {
                    content = content.replaceAll("secret: \".*?\"", "secret: \"" + secret + "\"");
                    Files.writeString(paperGlobal.toPath(), content);
                }
            }
        } catch (IOException ignored) {}
    }

    public String canonicalSecret() throws IOException {
        if (CANONICAL_SECRET_FILE.isFile()) {
            String secret = Files.readString(CANONICAL_SECRET_FILE.toPath()).trim();
            if (!secret.isEmpty()) return secret;
        }

        File parent = CANONICAL_SECRET_FILE.getParentFile();
        if (!parent.exists()) Files.createDirectories(parent.toPath());

        String secret = UUID.randomUUID().toString().replace("-", "");
        Files.writeString(CANONICAL_SECRET_FILE.toPath(), secret);
        return secret;
    }

    public String generateOrReadSecret(File proxyDir) throws IOException {
        String secret = canonicalSecret();
        File proxySecretFile = new File(proxyDir, "forwarding.secret");
        String existing = proxySecretFile.isFile() ? Files.readString(proxySecretFile.toPath()).trim() : null;
        if (!secret.equals(existing)) {
            Files.writeString(proxySecretFile.toPath(), secret);
        }
        return secret;
    }

    public int extractServerPort(File serverDir) {
        File propsFile = new File(serverDir, "server.properties");
        if (!propsFile.exists()) return 25565;
        try {
            for (String line : Files.readAllLines(propsFile.toPath())) {
                if (line.startsWith("server-port=")) {
                    return Integer.parseInt(line.split("=")[1].trim());
                }
            }
        } catch (Exception ignored) {}
        return 25565;
    }

    public int extractProxyPort(File proxyDir) {
        File configFile = new File(proxyDir, "velocity.toml");
        if (!configFile.exists()) return 25577;
        try {
            for (String line : Files.readAllLines(configFile.toPath())) {
                if (line.startsWith("bind =")) {
                    String part = line.split(":")[1].replace("\"", "").replace(">", "").trim();
                    return Integer.parseInt(part);
                }
            }
        } catch (Exception ignored) {}
        return 25577;
    }

    public String buildServersBlock(Set<String> allowedGroups) {
        StringBuilder entries = new StringBuilder();
        List<String> names = new ArrayList<>();

        File serversDir = new File("servers");
        if (serversDir.exists() && serversDir.isDirectory()) {
            File[] serverFolders = serversDir.listFiles(File::isDirectory);
            if (serverFolders != null) {
                for (File sf : serverFolders) {
                    if (new File(sf, "paper.jar").exists() && isVisibleToProxy(sf, allowedGroups)) {
                        String name = sf.getName().toLowerCase(Locale.ROOT);
                        entries.append("    ").append(name).append(" = \"127.0.0.1:").append(extractServerPort(sf)).append("\"\n");
                        names.add(name);
                    }
                }
            }
        }

        if (names.isEmpty()) {
            entries.append("    lobby = \"127.0.0.1:25565\"\n");
            names.add("lobby");
        }

        String tryArray = names.stream().map(name -> "\"" + name + "\"").collect(Collectors.joining(", "));
        entries.append("    try = [").append(tryArray).append("]\n");

        return entries.toString();
    }

    private String motdLine() {
        var settings = settingsManager.get();
        return "motd = \"<#00ff00>" + settings.networkName() + " - " + settings.motd() + "\"";
    }

    public void generateVelocityConfig(File proxyDir, int port, String secret) {
        File configFile = new File(proxyDir, "velocity.toml");
        if (configFile.exists()) return;

        String configContent = """
                config-version = "3.3"
                bind = "0.0.0.0:%d"
                %s
                show-max-players = 50
                online-mode = true
                player-info-forwarding-mode = "modern"
                forwarding-secret-file = "forwarding.secret"

                [servers]
                %s
                [forced-hosts]
                """.formatted(port, motdLine(), buildServersBlock(resolveProxyGroups(proxyDir)).stripTrailing());

        try {
            Files.writeString(configFile.toPath(), configContent);
        } catch (IOException ignored) {}
    }

    private static final Pattern MOTD_PATTERN = Pattern.compile("^motd = \".*\"$", Pattern.MULTILINE);

    public void updateAllProxyConfigs() {
        File proxiesDir = new File("proxies");
        if (!proxiesDir.exists() || !proxiesDir.isDirectory()) return;
        File[] proxyFolders = proxiesDir.listFiles(File::isDirectory);
        if (proxyFolders == null) return;

        for (File proxyFolder : proxyFolders) {
            File configFile = new File(proxyFolder, "velocity.toml");
            if (!configFile.exists()) continue;
            try {
                String content = Files.readString(configFile.toPath());
                content = MOTD_PATTERN.matcher(content).replaceFirst(Matcher.quoteReplacement(motdLine()));

                int serversIdx = content.indexOf("[servers]");
                int forcedHostsIdx = content.indexOf("[forced-hosts]");

                if (serversIdx != -1) {
                    String serversBlock = buildServersBlock(resolveProxyGroups(proxyFolder));
                    String beforeServers = content.substring(0, serversIdx);
                    String afterServers = (forcedHostsIdx != -1) ? content.substring(forcedHostsIdx) : "";

                    content = beforeServers + "[servers]\n" + serversBlock + "\n" + afterServers;
                }

                Files.writeString(configFile.toPath(), content);
            } catch (IOException ignored) {}
        }
    }
}