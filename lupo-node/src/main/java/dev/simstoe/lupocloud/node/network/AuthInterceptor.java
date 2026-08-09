package dev.simstoe.lupocloud.node.network;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.network.ClusterAuth;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.io.IOException;

public class AuthInterceptor implements ServerInterceptor {
    private final ServiceConfigHandler configHandler;

    public AuthInterceptor(ServiceConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String provided = headers.get(ClusterAuth.SECRET_KEY);
        Object remoteAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

        String expected;
        try {
            expected = configHandler.canonicalSecret();
        } catch (IOException e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Could not verify cluster secret."), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        if (provided == null || !provided.equals(expected)) {
            CloudLogger.error("Network client from " + remoteAddress + " sent an invalid secret.");
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing cluster secret."), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        CloudLogger.info("Network client authenticated from " + remoteAddress + ".");
        return next.startCall(call, headers);
    }
}
