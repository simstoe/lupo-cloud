package dev.simstoe.lupocloud.web.auth;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Component
public class WebAdminSecret {
    private static final File SECRET_FILE = new File("local", "web-admin.secret");

    private final String secret;
    private final boolean generated;

    public WebAdminSecret() {
        try {
            boolean existed = SECRET_FILE.isFile() && !Files.readString(SECRET_FILE.toPath()).isBlank();
            this.secret = loadOrGenerate();
            this.generated = !existed;
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
        return generated;
    }

    public boolean matches(String candidate) {
        return secret.equals(candidate);
    }

    /** The plaintext admin password. Only meant to be shown to the operator right after {@link #wasGenerated()}. */
    public String secret() {
        return secret;
    }

    /** True if this password was freshly generated on this startup (i.e. the very first start). */
    public boolean wasGenerated() {
        return generated;
    }
}
