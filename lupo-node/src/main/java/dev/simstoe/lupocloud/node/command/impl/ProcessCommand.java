package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.ServiceType;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.util.List;

public final class ProcessCommand implements ICommand {
    private final ICloudManager cloudManager;

    public ProcessCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @Override
    public String name() {
        return "process";
    }

    @Override
    public String[] aliases() {
        return new String[] { "service" };
    }

    @Override
    public String description() {
        return "Manages processes (Usage: process create [taskName])";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("create")) {
            CloudLogger.error("Usage: process create [taskName]");
            return;
        }

        if (args.length >= 3) {
            String taskName = args[2];
            if (cloudManager.task(taskName).isPresent()) {
                cloudManager.startFromTask(taskName);
                terminal.flush();
                return;
            }
            CloudLogger.error("Unknown task '" + taskName + "', falling back to the interactive assistant.");
        }

        LineReader reader = CloudLogger.lineReader();
        if (reader == null) {
            CloudLogger.error("LineReader is not available. Setup wizard cannot be started.");
            return;
        }

        CloudLogger.info("&b=== Process Setup Assistant ===");

        String typeInput;
        while (true) {
            typeInput = reader.readLine(CloudLogger.translateColorCodes("&rWhich type do you want to create? (&bpaper&r / &bproxy&r) » ")).trim().toLowerCase();
            if (typeInput.equals("paper") || typeInput.equals("proxy") || typeInput.equals("velocity")) {
                break;
            }
            CloudLogger.error("Invalid type! Please choose 'paper' or 'proxy'.");
        }

        boolean isProxy = typeInput.equals("proxy") || typeInput.equals("velocity");
        String templateName = chooseTemplate(reader, isProxy ? ServiceType.VELOCITY : ServiceType.PAPER);

        String nameInput;
        while (true) {
            nameInput = reader.readLine(CloudLogger.translateColorCodes("&rEnter the name for the process » ")).trim();
            if (!nameInput.isEmpty()) {
                if (cloudManager.service(nameInput).isPresent() || cloudManager.isRunning(nameInput)) {
                    CloudLogger.error("A process with this name already exists or is running!");
                    continue;
                }
                break;
            }
            CloudLogger.error("The name cannot be empty!");
        }

        int defaultPort = isProxy ? 25577 : 25565;
        int port = defaultPort;

        while (true) {
            String portInput = reader.readLine(CloudLogger.translateColorCodes("&rEnter the port for the " + (isProxy ? "proxy" : "paper server") + " &7(Default: " + defaultPort + ")&r » ")).trim();
            if (portInput.isEmpty()) {
                break;
            }
            try {
                int parsedPort = Integer.parseInt(portInput);

                boolean portInUse = cloudManager.activeServices().stream()
                        .anyMatch(service -> service.port() == parsedPort);

                if (portInUse) {
                    CloudLogger.error("Port " + parsedPort + " is already in use by another service! Please choose another one.");
                    continue;
                }

                port = parsedPort;
                break;
            } catch (NumberFormatException e) {
                CloudLogger.error("Please enter a valid number!");
            }
        }

        if (isProxy) {
            CloudLogger.info("&aStarting proxy &b" + nameInput + " &aon port &b" + port + "&a...");
            cloudManager.startProxy(nameInput, port, templateName);
        } else {
            CloudLogger.info("&aStarting paper server &b" + nameInput + " &aon port &b" + port + "&a...");
            cloudManager.startServer(nameInput, port, templateName);
        }

        terminal.flush();
    }

    private String chooseTemplate(LineReader reader, ServiceType type) {
        List<String> available = cloudManager.templates(type);
        if (available.isEmpty()) return null;

        CloudLogger.info("&7Available templates: &b" + String.join("&r, &b", available) + " &r(leave empty for a blank server)");
        String input = reader.readLine(CloudLogger.translateColorCodes("&rUse a template? » ")).trim();
        if (input.isEmpty()) return null;

        if (!available.contains(input)) {
            CloudLogger.error("Unknown template '" + input + "', starting without a template.");
            return null;
        }
        return input;
    }
}