package dev.simstoe.lupocloud.api.network;

import dev.simstoe.lupocloud.api.network.proto.ChannelMessage;
import dev.simstoe.lupocloud.api.network.proto.Frame;
import dev.simstoe.lupocloud.api.network.proto.NodeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class NetworkClient {
    private volatile ManagedChannel channel;
    private volatile StreamObserver<Frame> requestObserver;
    private volatile BiConsumer<String, String> channelMessageListener = (channelName, payload) -> {};

    public CompletableFuture<Void> connect(String host, int port, String secret) {
        CompletableFuture<Void> connected = new CompletableFuture<>();

        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .intercept(new SecretHeaderInterceptor(secret))
                .build();

        var stub = NodeServiceGrpc.newStub(channel);

        requestObserver = stub.link(new StreamObserver<>() {
            @Override
            public void onNext(Frame frame) {
                switch (frame.getPayloadCase()) {
                    case ACK -> connected.complete(null);
                    case CHANNEL_MESSAGE -> {
                        ChannelMessage message = frame.getChannelMessage();
                        channelMessageListener.accept(message.getChannel(), message.getPayload());
                    }
                    default -> {}
                }
            }

            @Override
            public void onError(Throwable t) {
                connected.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {}
        });

        return connected;
    }

    public void onChannelMessage(BiConsumer<String, String> listener) {
        this.channelMessageListener = listener;
    }

    public void sendChannelMessage(String channelName, String payload) {
        StreamObserver<Frame> current = requestObserver;
        if (current == null) return;
        current.onNext(Frame.newBuilder()
                .setChannelMessage(ChannelMessage.newBuilder().setChannel(channelName).setPayload(payload))
                .build());
    }

    public void disconnect() {
        StreamObserver<Frame> observer = requestObserver;
        if (observer != null) observer.onCompleted();

        ManagedChannel current = channel;
        if (current == null) return;
        current.shutdown();
        try {
            current.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
