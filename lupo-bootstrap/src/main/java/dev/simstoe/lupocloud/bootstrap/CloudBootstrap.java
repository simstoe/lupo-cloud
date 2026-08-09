package dev.simstoe.lupocloud.bootstrap;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.node.command.CommandHandler;
import dev.simstoe.lupocloud.node.console.CloudConsole;
import dev.simstoe.lupocloud.node.network.NodeServer;
import dev.simstoe.lupocloud.node.registry.CloudManagerImpl;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import dev.simstoe.lupocloud.node.registry.task.TaskManager;
import dev.simstoe.lupocloud.node.setup.SetupWizard;

public final class CloudBootstrap {
    static void main(String[] args) {
        var cloudManager = new CloudManagerImpl();

        var commandHandler = new CommandHandler(cloudManager);
        commandHandler.registerDefaultCommands();

        CloudConsole console;
        try {
            console = new CloudConsole(commandHandler);
        } catch (Exception e) {
            System.err.println("Fehler beim Initialisieren des Terminals: " + e.getMessage());
            return;
        }
        printBanner();



        var nodeServer = new NodeServer(new ServiceConfigHandler(new TaskManager()));
        nodeServer.start(NodeServer.DEFAULT_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cloudManager.stopAll();
            nodeServer.shutdown();
        }, "shutdown-hook"));

        new SetupWizard(cloudManager).runIfNeeded();

        console.loop();
    }

    private static void printBanner() {
        CloudLogger.plain("  _                              _____ _                 _ \n" +
                " | |                            / ____| |               | |\n" +
                " | |    _   _ _ __   ___ ______| |    | | ___  _   _  __| |\n" +
                " | |   | | | | '_ \\ / _ \\______| |    | |/ _ \\| | | |/ _` |\n" +
                " | |___| |_| | |_) | (_) |     | |____| | (_) | |_| | (_| |\n" +
                " |______\\__,_| .__/ \\___/       \\_____|_|\\___/ \\__,_|\\__,_|\n" +
                "             | |                                           \n" +
                "             |_|                                           ");

        CloudLogger.plain("CloudSystem - Version: &b2026-08-02.1&r");
        CloudLogger.plain("Author: &bSimon Stögerer &r(&bsimstoe &r- &bhttps://simstoe.dev&r)\n");
    }
}
