package dev.simstoe.lupocloud.api.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class CloudLogger {
    private static final Logger LOGGER = LogManager.getLogger(CloudLogger.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static LineReader lineReader;
    private static Terminal terminal;

    public static void setup(Terminal term, LineReader reader) {
        terminal = term;
        lineReader = reader;
    }

    public static void detachTerminal() {
        lineReader = null;
        terminal = null;
    }

    public static void info(String message) {
        String formattedMsg = translateColorCodes(message);
        LOGGER.info(getTime() + " | INFO - " + stripColorCodes(message));
        print(buildStyledMessage("INFO", formattedMsg, AttributedStyle.DEFAULT));
    }

    public static void success(String message) {
        String formattedMsg = translateColorCodes(message);
        LOGGER.info(getTime() + " | SUCCESS - " + stripColorCodes(message));
        print(buildStyledMessage("SUCCESS", formattedMsg, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)));
    }

    public static void error(String message) {
        String formattedMsg = translateColorCodes(message);
        LOGGER.error(getTime() + " | ERROR - " + stripColorCodes(message));
        print(buildStyledMessage("ERROR", formattedMsg, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)));
    }

    public static void plain(String message) {
        var formattedMessage = translateColorCodes(message);

        print(formattedMessage);
    }

    private static String getTime() {
        return TIME_FORMATTER.format(LocalTime.now());
    }

    public static String translateColorCodes(String message) {
        if (message == null) return "";
        return message
                .replace("&0", "\u001B[30m") // Schwarz
                .replace("&1", "\u001B[34m") // Dunkelblau
                .replace("&2", "\u001B[32m") // Dunkelgrün
                .replace("&3", "\u001B[36m") // Dunkeltürkis / Cyan
                .replace("&4", "\u001B[31m") // Dunkelrot
                .replace("&5", "\u001B[35m") // Lila
                .replace("&6", "\u001B[33m") // Gold / Orange
                .replace("&7", "\u001B[37m") // Grau
                .replace("&8", "\u001B[90m") // Dunkelgrau
                .replace("&9", "\u001B[94m") // Blau
                .replace("&a", "\u001B[92m") // Hellgrün
                .replace("&b", "\u001B[96m") // Cyan / Aqua (Hell)
                .replace("&c", "\u001B[91m") // Hellrot
                .replace("&d", "\u001B[95m") // Pink
                .replace("&e", "\u001B[93m") // Gelb
                .replace("&f", "\u001B[97m") // Weiß
                .replace("&r", "\u001B[0m"); // Reset
    }


    private static String stripColorCodes(String message) {
        if (message == null) return "";
        return message.replaceAll("&[0-9a-fk-or]", "");
    }

    private static String buildStyledMessage(String level, String coloredMessage, AttributedStyle levelStyle) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.bold())
                .append(getTime())
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK))
                .append(" | ")
                .style(levelStyle.bold())
                .append(level)
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK))
                .append(" - ")
                .style(AttributedStyle.DEFAULT)
                .append(coloredMessage)

                .append("\u001B[0m")
                .toAnsi();
    }

    /** @return true if the line went out through JLine, false if the caller should fall back to stdout. */
    private static boolean writeToTerminal(String coloredText) {
        try {
            if (lineReader != null) {
                lineReader.printAbove(coloredText);
                return true;
            }
            if (terminal != null) {
                terminal.writer().println(coloredText);
                return true;
            }
        } catch (RuntimeException e) {
            // Terminal died under us (closed console, broken pipe): stop using it for good, so that a
            // log call can never break its caller - e.g. an API request that happens to log something.
            detachTerminal();
        }
        return false;
    }

    private static void print(String coloredText) {
        if (writeToTerminal(coloredText)) return;
        if (lineReader == null && terminal == null) {
            System.out.println(coloredText.replaceAll("\u001B\\[[;\\d]*m", ""));
        }
    }

    public static LineReader lineReader() {
        return lineReader;
    }
}