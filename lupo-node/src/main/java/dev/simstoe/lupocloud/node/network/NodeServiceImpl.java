package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.network.proto.Ack;
import dev.simstoe.lupocloud.api.network.proto.Frame;
import dev.simstoe.lupocloud.api.network.proto.NodeServiceGrpc;
import dev.simstoe.lupocloud.api.network.proto.Pong;
import io.grpc.stub.StreamObserver;

public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {
    private final ConnectionRegistry registry;

    public NodeServiceImpl(ConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public StreamObserver<Frame> link(StreamObserver<Frame> responseObserver) {
        registry.register(responseObserver);
        registry.send(responseObserver, Frame.newBuilder().setAck(Ack.getDefaultInstance()).build());

        return new StreamObserver<>() {
            @Override
            public void onNext(Frame frame) {
                switch (frame.getPayloadCase()) {
                    case CHANNEL_MESSAGE -> registry.broadcast(frame, responseObserver);
                    case PING -> registry.send(responseObserver, Frame.newBuilder().setPong(Pong.getDefaultInstance()).build());
                    default -> {}
                }
            }

            @Override
            public void onError(Throwable t) {
                registry.unregister(responseObserver);
            }

            @Override
            public void onCompleted() {
                registry.unregister(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }
}
