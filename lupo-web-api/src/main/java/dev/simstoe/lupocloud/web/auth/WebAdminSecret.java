package dev.simstoe.lupocloud.web.auth;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Component
public class WebAdminSecret {
    private static final File SECRET_FILE = new File("local", "web-admin.secret");

    private final String secret;

    public WebAdminSecret() {
        try {
            this.secret = loadOrGenerate();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read/generate the web admin secret.", e);
        }
    }

    private String loadOrGenerate() throws IOException {
        if (SECRET_FILE.isFile()) {
            String existing = Files.readString(SECRET_FILE.toPath()).trim();
            if (!existing.isEmpty()) return existing;
        }

        File parent = SECRET_FILE.getParentFile();
        if (!parent.exists()) Files.createDirectories(parent.toPath());

        String generated = UUID.randomUUID().toString().replace("-", "");
        Files.writeString(SECRET_FILE.toPath(), generated);
        CloudLogger.success("Generated web admin password: " + generated + " (saved to " + SECRET_FILE.getPath() + ")");
        return generated;
    }

    public boolean matches(String candidate) {
        return secret.equals(candidate);
    }
}
