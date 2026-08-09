package dev.simstoe.lupocloud.web;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;

public final class WebApiServer {
    public static final int DEFAULT_PORT = 8080;

    private WebApiServer() {}

    public static ConfigurableApplicationContext start(ICloudManager cloudManager, int port) {
        return new SpringApplicationBuilder(WebApiApplication.class)
                .web(WebApplicationType.SERVLET)
                .initializers(ctx -> ((GenericApplicationContext) ctx)
                        .registerBean(ICloudManager.class, () -> cloudManager))
                .properties(Map.of("server.port", String.valueOf(port)))
                .run();
    }
}
