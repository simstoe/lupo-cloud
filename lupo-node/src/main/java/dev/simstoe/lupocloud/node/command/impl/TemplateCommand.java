package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

import java.util.List;

public class TemplateCommand implements ICommand {
    private final ICloudManager cloudManager;

    public TemplateCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @Override
    public String name() {
        return "template";
    }

    @Override
    public String[] aliases() {
        return new String[] { "tpl" };
    }

    @Override
    public String description() {
        return "Manage templates (Usage: template save|list|delete)";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        switch (args[1].toLowerCase()) {
            case "save" -> {
                if (args.length < 4) {
                    CloudLogger.error("Usage: template save <server> <templateName>");
                    return;
                }
                cloudManager.saveTemplate(args[3], args[2]);
            }
            case "list" -> {
                List<String> templates = cloudManager.templates();
                if (templates.isEmpty()) {
                    CloudLogger.info("No templates available yet. Create one with 'template save <server> <name>'.");
                } else {
                    CloudLogger.info("Available templates:");
                    templates.forEach(t -> CloudLogger.info(" - &b" + t));
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    CloudLogger.error("Usage: template delete <templateName>");
                    return;
                }
                cloudManager.deleteTemplate(args[2]);
            }
            default -> printUsage();
        }

        terminal.flush();
    }

    private void printUsage() {
        CloudLogger.error("Usage: template <save <server> <name> | list | delete <name>>");
    }
}
