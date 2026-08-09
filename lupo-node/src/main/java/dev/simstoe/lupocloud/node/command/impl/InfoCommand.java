package dev.simstoe.lupocloud.node.command.impl;


import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

public class InfoCommand implements ICommand {
    @Override
    public String name() {
        return "info";
    }

    @Override
    public String description() {
        return "Show detailed information about the cloud";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        CloudLogger.info("Cloud information:");
        CloudLogger.info(" Java Version: " + System.getProperty("java.version"));
        CloudLogger.info(" Java Vendor: " + System.getProperty("java.vendor"));
        CloudLogger.info(" OS Name: " + System.getProperty("os.name"));
        CloudLogger.info(" OS Architecture: " + System.getProperty("os.arch"));
        CloudLogger.info(" Available Processors: " + Runtime.getRuntime().availableProcessors());

        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        CloudLogger.info(" Memory Usage: " + usedMemory + " MB / " + totalMemory + " MB (Max: " + maxMemory + " MB)");
    }
}
