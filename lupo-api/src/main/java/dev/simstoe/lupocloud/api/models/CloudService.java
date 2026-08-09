package dev.simstoe.lupocloud.api.models;

import java.util.UUID;

public record CloudService(String name, ServiceType type, int port, UUID uuid, boolean running) {
    public static CloudService create(String name, ServiceType type, int port) {
        return new CloudService(name, type, port, UUID.randomUUID(), false);
    }

    public CloudService running(boolean running) {
        return new CloudService(name, type, port, uuid, running);
    }
}