package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.network.Packet;
import io.netty.channel.Channel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {
    private final Set<Channel> authenticated = ConcurrentHashMap.newKeySet();

    public void register(Channel channel) {
        authenticated.add(channel);
    }

    public void unregister(Channel channel) {
        authenticated.remove(channel);
    }

    public int connectionCount() {
        return authenticated.size();
    }

    public void broadcast(Packet packet, Channel exclude) {
        for (Channel channel : authenticated) {
            if (channel != exclude && channel.isActive()) {
                channel.writeAndFlush(packet);
            }
        }
    }
}
