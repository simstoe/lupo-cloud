package dev.simstoe.lupocloud.api.network;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class PacketEncoder extends MessageToByteEncoder<Packet> {
    private static final Gson GSON = new Gson();

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
        out.writeBytes(GSON.toJson(packet).getBytes(StandardCharsets.UTF_8));
    }
}
