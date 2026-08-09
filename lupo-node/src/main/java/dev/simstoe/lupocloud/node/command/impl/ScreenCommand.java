package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

import java.util.List;

public class ScreenCommand implements ICommand {
    private final ICloudManager cloudManager;

    public ScreenCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }


    @Override
    public String name() {
        return "screen";
    }

    @Override
    public String description() {
        return "Open the console of a specific server";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        if (args.length < 2) {
            CloudLogger.error("Usage: screen <serverName>");
            return;
        }

        String serverName = args[1];

        if (!this.cloudManager.isRunning(serverName)) {
            CloudLogger.error("No running server found with the name: " + serverName);
            return;
        }

        CloudLogger.info("=== Connected to server console: " + serverName + " ===");
        CloudLogger.info("Type 'exit' or 'detach' to leave the screen.");

        List<String> history = this.cloudManager.serviceLogs(serverName);
        for (String line : history) {
            terminal.writer().println(line);
        }
        terminal.writer().flush();

        var lineReader = CloudLogger.lineReader();
        if (lineReader == null) {
            CloudLogger.error("No LineReader available for screen mode.");
            return;
        }

        while (true) {
            try {
                String input = lineReader.readLine("[" + serverName + "] > ");
                if (input == null) break;

                input = input.trim();
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("detach")) {
                    CloudLogger.info("Left screen mode.");
                    break;
                }

                if (!input.isEmpty()) {
                    this.cloudManager.sendCommand(serverName, input);
                }
            } catch (Exception e) {
                break;
            }
        }
    }
}