package dev.simstoe.lupocloud.web.config;

import dev.simstoe.lupocloud.web.console.ConsoleAuthHandshakeInterceptor;
import dev.simstoe.lupocloud.web.console.ConsoleWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ConsoleWebSocketHandler consoleWebSocketHandler;
    private final ConsoleAuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(ConsoleWebSocketHandler consoleWebSocketHandler,
                            ConsoleAuthHandshakeInterceptor authHandshakeInterceptor) {
        this.consoleWebSocketHandler = consoleWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consoleWebSocketHandler, "/ws/console/*")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("http://localhost:3000");
    }
}
