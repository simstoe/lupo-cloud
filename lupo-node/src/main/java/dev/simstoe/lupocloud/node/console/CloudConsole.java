package dev.simstoe.lupocloud.node.console;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.node.command.CommandHandler;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;

public final class CloudConsole {

    private final Terminal terminal;
    private final LineReader reader;
    private final CommandHandler commandHandler;

    public CloudConsole(CommandHandler commandHandler) throws IOException {
        this.commandHandler = commandHandler;
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(commandHandler.createCompleter())
                .build();

        CloudLogger.setup(this.terminal, this.reader);
    }

    public void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    public void loop() {
        try {
            var prompt = "\u001B[96mroot\u001B[0m@\u001B[96mcloud\u001B[0m » ";

            while (true) {
                try {
                    String line = reader.readLine(prompt).trim();
                    if (line.isEmpty()) continue;

                    if (commandHandler.handle(terminal, line)) {
                        break;
                    }

                } catch (UserInterruptException e) {
                    CloudLogger.info("&c^C gedrückt. Zum Beenden 'exit' eingeben.");
                } catch (EndOfFileException e) {
                    break;
                }
            }
        } finally {
            closeTerminal();
        }
    }

    private void closeTerminal() {
        // Headless (Docker without a tty) readLine() hits EOF immediately and we end up here while the
        // web API keeps serving. Detach first so later log calls go to stdout instead of a dead terminal.
        CloudLogger.detachTerminal();
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException ignored) {}
        }
    }
}
