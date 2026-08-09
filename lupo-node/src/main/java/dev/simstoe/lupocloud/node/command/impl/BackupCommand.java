package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

import java.util.List;
import java.util.Locale;

public class BackupCommand implements ICommand {
    private final ICloudManager cloudManager;

    public BackupCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @Override
    public String name() {
        return "backup";
    }

    @Override
    public String description() {
        return "Manage backups (Usage: backup create|list|restore|delete)";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        if (args.length < 3) {
            printUsage();
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> cloudManager.createBackup(args[2]);
            case "list" -> listBackups(args[2]);
            case "restore" -> {
                if (args.length < 4) {
                    CloudLogger.error("Usage: backup restore <service> <backupFile>");
                    return;
                }
                cloudManager.restoreBackup(args[2], args[3]);
            }
            case "delete" -> {
                if (args.length < 4) {
                    CloudLogger.error("Usage: backup delete <service> <backupFile>");
                    return;
                }
                cloudManager.deleteBackup(args[2], args[3]);
            }
            default -> printUsage();
        }

        terminal.flush();
    }

    private void listBackups(String serviceName) {
        List<String> backups = cloudManager.backups(serviceName);
        if (backups.isEmpty()) {
            CloudLogger.info("No backups available yet for '" + serviceName + "'.");
        } else {
            CloudLogger.info("Available backups for '" + serviceName + "':");
            backups.forEach(b -> CloudLogger.info(" - &b" + b));
        }
    }

    private void printUsage() {
        CloudLogger.error("Usage: backup <create <service> | list <service> | restore <service> <backupFile> | delete <service> <backupFile>>");
    }
}
