package dev.simstoe.lupocloud.node.registry;

import dev.simstoe.lupocloud.api.models.CloudService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;

public class PortAllocator {
    private static final int MAX_ATTEMPTS = 1000;

    public int allocate(int startPort, Collection<CloudService> activeServices) {
        for (int port = startPort; port < startPort + MAX_ATTEMPTS && port <= 65535; port++) {
            if (isUsedByActiveService(port, activeServices)) continue;
            if (isBoundOnHost(port)) continue;
            return port;
        }
        throw new IllegalStateException("No free port found starting from " + startPort + ".");
    }

    private boolean isUsedByActiveService(int port, Collection<CloudService> activeServices) {
        return activeServices.stream().anyMatch(service -> service.port() == port);
    }

    private boolean isBoundOnHost(int port) {
        try (var socket = new ServerSocket(port, 1, InetAddress.getByName("0.0.0.0"))) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
