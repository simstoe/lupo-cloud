package dev.simstoe.lupocloud.api.models;

public record NetworkSettings(String networkName, String motd) {
    public static NetworkSettings defaults() {
        return new NetworkSettings("Lupo Cloud", "A Minecraft Cloud Network");
    }
}
