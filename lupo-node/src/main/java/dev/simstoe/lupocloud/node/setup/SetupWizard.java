package dev.simstoe.lupocloud.node.setup;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import dev.simstoe.lupocloud.api.models.ServiceType;
import org.jline.reader.LineReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

public class SetupWizard {
    private static final File MARKER_FILE = new File("local", ".setup-done");

    private final ICloudManager cloudManager;

    public SetupWizard(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    public void runIfNeeded() {

        if (MARKER_FILE.isFile()) return;
        if (!looksLikeFreshInstall()) {
            markDone();
            return;
        }

        LineReader reader = CloudLogger.lineReader();
        if (reader == null) {
            markDone();
            return;
        }

        CloudLogger.info("&b=== First-Time Setup ===");
        CloudLogger.info("&7No tasks are configured yet. I can set up a default proxy and lobby server for you.");

        if (!promptYesNo(reader, "Set up a default proxy + lobby now?")) {
            CloudLogger.info("Skipped. You can always create tasks later with 'task create'.");
            markDone();
            return;
        }

        String proxyName = promptWithDefault(reader, "Proxy task name", "proxy");
        int proxyPort = promptPortWithDefault(reader, "Proxy port", 25565);
        String lobbyName = promptWithDefault(reader, "Lobby task name", "lobby");
        int lobbyPort = promptPortWithDefault(reader, "Lobby port", 25580);


        cloudManager.createTask(asStatic(ServiceTask.create(proxyName, ServiceType.VELOCITY, proxyPort)));
        cloudManager.createTask(asStatic(ServiceTask.create(lobbyName, ServiceType.PAPER, lobbyPort)));

        CloudLogger.success("First-time setup complete!");
        markDone();

        if (promptYesNo(reader, "Start them now?")) {
            cloudManager.startFromTask(lobbyName);
            cloudManager.startFromTask(proxyName);
        }
    }

    private ServiceTask asStatic(ServiceTask task) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), true, true,
                task.group(), task.proxyGroups());
    }

    private boolean looksLikeFreshInstall() {
        if (!cloudManager.tasks().isEmpty()) return false;
        if (hasSubdirectories(new File("servers"))) return false;
        return !hasSubdirectories(new File("proxies"));
    }

    private boolean hasSubdirectories(File dir) {
        File[] children = dir.listFiles(File::isDirectory);
        return children != null && children.length > 0;
    }

    private boolean promptYesNo(LineReader reader, String question) {
        String answer = reader.readLine(CloudLogger.translateColorCodes("&r" + question + " &7(Y/n)&r » ")).trim();
        return !answer.equalsIgnoreCase("n") && !answer.equalsIgnoreCase("no");
    }

    private String promptWithDefault(LineReader reader, String label, String defaultValue) {
        String input = reader.readLine(CloudLogger.translateColorCodes("&r" + label + " &7(default: " + defaultValue + ")&r » ")).trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private int promptPortWithDefault(LineReader reader, String label, int defaultValue) {
        while (true) {
            String input = reader.readLine(CloudLogger.translateColorCodes("&r" + label + " &7(default: " + defaultValue + ")&r » ")).trim();
            if (input.isEmpty()) return defaultValue;
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                CloudLogger.error("Please enter a valid number!");
            }
        }
    }

    private void markDone() {
        try {
            File parent = MARKER_FILE.getParentFile();
            if (!parent.exists()) Files.createDirectories(parent.toPath());
            Files.writeString(MARKER_FILE.toPath(), Instant.now().toString());
        } catch (IOException e) {
            CloudLogger.error("Could not persist the first-time setup marker: " + e.getMessage());
        }
    }
}
