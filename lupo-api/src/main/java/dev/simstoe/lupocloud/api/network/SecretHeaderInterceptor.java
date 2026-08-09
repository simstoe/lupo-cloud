package dev.simstoe.lupocloud.api.network;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class SecretHeaderInterceptor implements ClientInterceptor {
    private final String secret;

    public SecretHeaderInterceptor(String secret) {
        this.secret = secret;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(ClusterAuth.SECRET_KEY, secret);
                super.start(responseListener, headers);
            }
        };
    }
}
