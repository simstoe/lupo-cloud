package dev.simstoe.lupocloud.api.network;

import io.grpc.Metadata;

public final class ClusterAuth {
    public static final Metadata.Key<String> SECRET_KEY =
            Metadata.Key.of("x-cluster-secret", Metadata.ASCII_STRING_MARSHALLER);

    private ClusterAuth() {}
}
