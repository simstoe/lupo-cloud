package dev.simstoe.lupocloud.api.network;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public final class PacketPipeline {
    private static final int MAX_FRAME_LENGTH = 4 * 1024 * 1024;
    private static final int LENGTH_FIELD_LENGTH = 4;

    private PacketPipeline() {}

    public static void configure(ChannelPipeline pipeline) {
        pipeline.addLast(new LengthFieldPrepender(LENGTH_FIELD_LENGTH));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, LENGTH_FIELD_LENGTH, 0, LENGTH_FIELD_LENGTH));
        pipeline.addLast(new PacketEncoder());
        pipeline.addLast(new PacketDecoder());
    }
}
