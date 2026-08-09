package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.network.Packet;
import dev.simstoe.lupocloud.api.network.PacketType;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

public class NodeConnectionHandler extends SimpleChannelInboundHandler<Packet> {
    private final ServiceConfigHandler configHandler;
    private final ConnectionRegistry registry;
    private boolean authenticated = false;

    public NodeConnectionHandler(ServiceConfigHandler configHandler, ConnectionRegistry registry) {
        this.configHandler = configHandler;
        this.registry = registry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        if (!authenticated) {
            handleAuth(ctx, packet);
            return;
        }

        switch (packet.type()) {
            case CHANNEL_MESSAGE -> registry.broadcast(packet, ctx.channel());
            case PING -> ctx.writeAndFlush(Packet.pong());
            default -> {}
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, Packet packet) {
        if (packet.type() != PacketType.AUTH) {
            ctx.close();
            return;
        }

        String expectedSecret;
        try {
            expectedSecret = configHandler.canonicalSecret();
        } catch (IOException e) {
            ctx.writeAndFlush(Packet.authFailed()).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        if (expectedSecret.equals(packet.payload())) {
            authenticated = true;
            registry.register(ctx.channel());
            ctx.writeAndFlush(Packet.authOk());
            CloudLogger.info("Network client authenticated from " + ctx.channel().remoteAddress() + ".");
        } else {
            CloudLogger.error("Network client from " + ctx.channel().remoteAddress() + " sent an invalid secret.");
            ctx.writeAndFlush(Packet.authFailed()).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        registry.unregister(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        registry.unregister(ctx.channel());
        ctx.close();
    }
}
