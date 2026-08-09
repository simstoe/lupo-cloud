package dev.simstoe.lupocloud.node.command;

import org.jline.terminal.Terminal;

public interface ICommand extends Comparable<ICommand> {
    String name();

    default String[] aliases() {
        return new String[0];
    }

    String description();

    void execute(Terminal terminal, String[] args);

    default int compareTo(ICommand other) {
        return this.name().compareTo(other.name());
    }
}
