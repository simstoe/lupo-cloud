package dev.simstoe.lupocloud.web.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ConsoleWebSocketHandler extends TextWebSocketHandler {
    private final ICloudManager cloudManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> pollTasks = new ConcurrentHashMap<>();

    public ConsoleWebSocketHandler(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    private String serviceName(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String name = serviceName(session);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> pushSnapshot(session, name), 0, 1, TimeUnit.SECONDS);
        pollTasks.put(session.getId(), future);
    }

    private void pushSnapshot(WebSocketSession session, String name) {
        if (!session.isOpen()) return;
        try {
            List<String> lines = cloudManager.serviceLogs(name);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(lines)));
        } catch (IOException ignored) {
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        cloudManager.sendCommand(serviceName(session), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ScheduledFuture<?> future = pollTasks.remove(session.getId());
        if (future != null) future.cancel(true);
    }
}
