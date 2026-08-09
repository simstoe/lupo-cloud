package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

public class ClearCommand implements ICommand {
    @Override
    public String name() {
        return "clear";
    }

    @Override
    public String description() {
        return "Clears the console screen";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
}
