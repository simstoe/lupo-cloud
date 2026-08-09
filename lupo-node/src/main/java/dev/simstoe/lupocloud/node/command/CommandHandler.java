package dev.simstoe.lupocloud.node.command;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.node.command.impl.*;
import dev.simstoe.lupocloud.node.command.impl.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {
    private final Map<String, ICommand> commands = new HashMap<>();
    private final ICloudManager cloudManager;

    public CommandHandler(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    public void registerCommand(ICommand command) {
        commands.put(command.name().toLowerCase(), command);

        for (var alias : command.aliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    public void registerDefaultCommands() {
        this.registerCommand(new InfoCommand());
        this.registerCommand(new HelpCommand(this.commands));
        this.registerCommand(new ClearCommand());
        this.registerCommand(new ProcessCommand(this.cloudManager));
        this.registerCommand(new ExitCommand(this.cloudManager));
        this.registerCommand(new ScreenCommand(this.cloudManager));
        this.registerCommand(new TemplateCommand(this.cloudManager));
        this.registerCommand(new TaskCommand(this.cloudManager));
        this.registerCommand(new BackupCommand(this.cloudManager));
    }

    public StringsCompleter createCompleter() {
        return new StringsCompleter(commands.keySet());
    }

    public boolean handle(Terminal terminal, String input) {
        var parts = input.trim().split("\\s+");
        var commandName = parts[0].toLowerCase();

        var command = commands.get(commandName);

        if (command != null) {
            command.execute(terminal, parts);
            return command.name().equals("exit");
        } else {
            terminal.writer().println("Unbekannter Befehl: " + commandName + " (Tippe 'hilfe')");
            return false;
        }
    }
}