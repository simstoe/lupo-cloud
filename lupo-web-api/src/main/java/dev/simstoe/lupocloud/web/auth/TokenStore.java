package dev.simstoe.lupocloud.web.auth;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();

    public String issue() {
        String token = UUID.randomUUID().toString();
        tokens.add(token);
        return token;
    }

    public boolean isValid(String token) {
        return token != null && tokens.contains(token);
    }
}
