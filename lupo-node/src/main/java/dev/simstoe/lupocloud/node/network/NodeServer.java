package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.concurrent.TimeUnit;

public class NodeServer {
    public static final int DEFAULT_PORT = 25555;

    private final ServiceConfigHandler configHandler;
    private final ConnectionRegistry registry = new ConnectionRegistry();

    private Server server;

    public NodeServer(ServiceConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    public void start(int port) {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new NodeServiceImpl(registry))
                    .intercept(new AuthInterceptor(configHandler))
                    .build()
                    .start();
            CloudLogger.success("Network server listening on port " + port + ".");
        } catch (Exception e) {
            CloudLogger.error("Could not start network server on port " + port + ": " + e.getMessage());
            shutdown();
        }
    }

    public void shutdown() {
        if (server == null) return;
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
    }

    public ConnectionRegistry registry() {
        return registry;
    }
}
