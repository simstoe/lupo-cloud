package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.network.PacketPipeline;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NodeServer {
    public static final int DEFAULT_PORT = 25555;

    private final ServiceConfigHandler configHandler;
    private final ConnectionRegistry registry = new ConnectionRegistry();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NodeServer(ServiceConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            PacketPipeline.configure(ch.pipeline());
                            ch.pipeline().addLast(new NodeConnectionHandler(configHandler, registry));
                        }
                    });

            serverChannel = bootstrap.bind(port).sync().channel();
            CloudLogger.success("Network server listening on port " + port + ".");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CloudLogger.error("Could not start network server: " + e.getMessage());
            shutdown();
        } catch (Exception e) {
            CloudLogger.error("Could not start network server on port " + port + ": " + e.getMessage());
            shutdown();
        }
    }

    public void shutdown() {
        if (serverChannel != null) serverChannel.close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }

    public ConnectionRegistry registry() {
        return registry;
    }
}
