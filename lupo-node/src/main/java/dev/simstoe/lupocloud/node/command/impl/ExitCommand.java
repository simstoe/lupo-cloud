package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

public class ExitCommand implements ICommand {
    private final ICloudManager cloudManager;

    public ExitCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @Override
    public String name() {
        return "exit";
    }

    @Override
    public String[] aliases() {
        return new String[] { "quit", "shutdown", "stop" };
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        CloudLogger.info("Initiating shutdown. Waiting for all tasks to complete...");
        this.cloudManager.stopAll();
        CloudLogger.info("All tasks completed. Exiting the application. Goodbye!");
        terminal.flush();
        System.exit(0);
    }
}
