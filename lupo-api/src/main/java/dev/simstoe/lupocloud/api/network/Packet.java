package dev.simstoe.lupocloud.api.network;

public record Packet(PacketType type, String channel, String payload) {
    public static Packet auth(String secret) {
        return new Packet(PacketType.AUTH, null, secret);
    }

    public static Packet authOk() {
        return new Packet(PacketType.AUTH_OK, null, null);
    }

    public static Packet authFailed() {
        return new Packet(PacketType.AUTH_FAILED, null, null);
    }

    public static Packet channelMessage(String channel, String payload) {
        return new Packet(PacketType.CHANNEL_MESSAGE, channel, payload);
    }

    public static Packet ping() {
        return new Packet(PacketType.PING, null, null);
    }

    public static Packet pong() {
        return new Packet(PacketType.PONG, null, null);
    }
}
