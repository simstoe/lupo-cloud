package dev.simstoe.lupocloud.api.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class NetworkClient {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private volatile Channel channel;
    private volatile Consumer<Packet> packetListener = packet -> {};

    public CompletableFuture<Void> connect(String host, int port, String secret) {
        CompletableFuture<Void> connected = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        PacketPipeline.configure(ch.pipeline());
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<Packet>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
                                if (packet.type() == PacketType.AUTH_OK) {
                                    connected.complete(null);
                                } else if (packet.type() == PacketType.AUTH_FAILED) {
                                    connected.completeExceptionally(new IllegalStateException("Authentication failed."));
                                    ctx.close();
                                } else {
                                    packetListener.accept(packet);
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                connected.completeExceptionally(cause);
                                ctx.close();
                            }
                        });
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                connected.completeExceptionally(future.cause());
                return;
            }
            channel = future.channel();
            channel.writeAndFlush(Packet.auth(secret));
        });

        return connected;
    }

    public void onPacket(Consumer<Packet> listener) {
        this.packetListener = listener;
    }

    public void send(Packet packet) {
        Channel current = channel;
        if (current != null && current.isActive()) {
            current.writeAndFlush(packet);
        }
    }

    public void sendChannelMessage(String channelName, String payload) {
        send(Packet.channelMessage(channelName, payload));
    }

    public void disconnect() {
        Channel current = channel;
        if (current != null) current.close();
        group.shutdownGracefully();
    }
}
