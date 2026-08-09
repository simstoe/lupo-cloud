package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

import java.util.Map;

public final class HelpCommand implements ICommand {
    private final Map<String, ICommand> commands;

    public HelpCommand(Map<String, ICommand> commands) {
        this.commands = commands;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String[] aliases() {
        return new String[] { "?" };
    }

    @Override
    public String description() {
        return "List all available commands";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        CloudLogger.info("Available commands:");

        var uniqueCommands = this.commands.values().stream()
                .distinct()
                .sorted()
                .map(command -> {
                    String[] aliases = command.aliases();
                    String aliasInfo = (aliases != null && aliases.length > 0)
                            ? " (&b" + String.join("&r, &b", aliases) + "&r)"
                            : "";

                    return String.format("&b%s&r%s: %s", command.name(), aliasInfo, command.description());
                })
                .toList();

        uniqueCommands.forEach(CloudLogger::info);

        terminal.flush();
    }
}