package dev.simstoe.lupocloud.api.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Gson GSON = new Gson();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        String json = new String(bytes, StandardCharsets.UTF_8);

        try {
            Packet packet = GSON.fromJson(json, Packet.class);
            if (packet != null) out.add(packet);
        } catch (JsonSyntaxException e) {
            ctx.close();
        }
    }
}
