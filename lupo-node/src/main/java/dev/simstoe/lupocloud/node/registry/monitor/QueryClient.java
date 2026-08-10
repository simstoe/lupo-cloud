package dev.simstoe.lupocloud.node.registry.monitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class QueryClient {
    private static final byte[] MAGIC = {(byte) 0xFE, (byte) 0xFD};
    private static final int TIMEOUT_MS = 1500;

    public record QueryResult(int online, int max, List<String> players) {}

    public Optional<QueryResult> query(String host, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(host);
            int sessionId = new Random().nextInt() & 0x0F0F0F0F;

            byte[] challengeToken = handshake(socket, address, port, sessionId);
            if (challengeToken == null) return Optional.empty();

            return fullStat(socket, address, port, sessionId, challengeToken);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private byte[] handshake(DatagramSocket socket, InetAddress address, int port, int sessionId) throws IOException {
        ByteBuffer request = ByteBuffer.allocate(7);
        request.put(MAGIC).put((byte) 0x09).putInt(sessionId);
        send(socket, address, port, request.array());

        byte[] buf = new byte[256];
        DatagramPacket response = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(response);
        } catch (SocketTimeoutException e) {
            return null;
        }

        String tokenStr = readCString(response.getData(), 5, response.getLength());
        if (tokenStr == null) return null;
        try {
            return ByteBuffer.allocate(4).putInt(Integer.parseInt(tokenStr.trim())).array();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Optional<QueryResult> fullStat(DatagramSocket socket, InetAddress address, int port, int sessionId, byte[] challengeToken) throws IOException {
        ByteBuffer request = ByteBuffer.allocate(15);
        request.put(MAGIC).put((byte) 0x00).putInt(sessionId).put(challengeToken).putInt(0);
        send(socket, address, port, request.array());

        byte[] buf = new byte[4096];
        DatagramPacket response = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(response);
        } catch (SocketTimeoutException e) {
            return Optional.empty();
        }

        byte[] data = response.getData();
        int length = response.getLength();
        int cursor = 5 + 11; // type(1) + sessionId(4) + "splitnum\0\x80\0" padding

        int online = 0;
        int max = 0;
        while (cursor < length) {
            String key = readCString(data, cursor, length);
            if (key == null) break;
            cursor += key.length() + 1;
            if (key.isEmpty()) break;

            String value = readCString(data, cursor, length);
            if (value == null) break;
            cursor += value.length() + 1;

            if (key.equals("numplayers")) online = parseIntSafe(value);
            else if (key.equals("maxplayers")) max = parseIntSafe(value);
        }

        cursor += 10; // "\x01player_\x00\x00" padding

        List<String> players = new ArrayList<>();
        while (cursor < length) {
            String name = readCString(data, cursor, length);
            if (name == null || name.isEmpty()) break;
            cursor += name.length() + 1;
            players.add(name);
        }

        return Optional.of(new QueryResult(online, max, players));
    }

    private static void send(DatagramSocket socket, InetAddress address, int port, byte[] data) throws IOException {
        socket.send(new DatagramPacket(data, data.length, address, port));
    }

    private static String readCString(byte[] data, int offset, int length) {
        int end = offset;
        while (end < length && data[end] != 0) end++;
        if (offset > length || end >= length) return null;
        return new String(data, offset, end - offset, StandardCharsets.UTF_8);
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
