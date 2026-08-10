package dev.simstoe.lupocloud.api.models;

/**
 * @param maxMemoryMB cloud-wide RAM budget: services whose -Xmx would push the total past this
 *                    are refused a start. {@code null} means no limit.
 */
public record NetworkSettings(String networkName, String motd, Integer maxMemoryMB) {
    public static NetworkSettings defaults() {
        return new NetworkSettings("Lupo Cloud", "A Minecraft Cloud Network", null);
    }
}
