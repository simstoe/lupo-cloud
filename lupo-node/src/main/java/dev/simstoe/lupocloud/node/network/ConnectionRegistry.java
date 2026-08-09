package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.network.proto.Frame;
import io.grpc.stub.StreamObserver;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {
    private final Set<StreamObserver<Frame>> connections = ConcurrentHashMap.newKeySet();

    public void register(StreamObserver<Frame> connection) {
        connections.add(connection);
    }

    public void unregister(StreamObserver<Frame> connection) {
        connections.remove(connection);
    }

    public int connectionCount() {
        return connections.size();
    }

    public void send(StreamObserver<Frame> target, Frame frame) {
        synchronized (target) {
            target.onNext(frame);
        }
    }

    public void broadcast(Frame frame, StreamObserver<Frame> exclude) {
        for (StreamObserver<Frame> connection : connections) {
            if (connection == exclude) continue;
            send(connection, frame);
        }
    }
}
