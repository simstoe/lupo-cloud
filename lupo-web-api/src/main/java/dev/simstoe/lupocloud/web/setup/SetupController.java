package dev.simstoe.lupocloud.web.setup;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.web.auth.AdminAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Instant;

@RestController
@RequestMapping("/api/setup")
public class SetupController {
    private static final File MARKER_FILE = new File("local", ".setup-done");

    private final ICloudManager cloudManager;
    private final AdminAccountService adminAccount;

    public SetupController(ICloudManager cloudManager, AdminAccountService adminAccount) {
        this.cloudManager = cloudManager;
        this.adminAccount = adminAccount;
    }

    @GetMapping("/status")
    public SetupStatus status() {
        return new SetupStatus(needsSetup(), !adminAccount.exists());
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> complete() {
        markDone();
        return ResponseEntity.noContent().build();
    }

    private boolean needsSetup() {
        if (MARKER_FILE.isFile()) return false;
        if (!cloudManager.tasks().isEmpty()) return false;
        if (hasSubdirectories(new File("servers"))) return false;
        return !hasSubdirectories(new File("proxies"));
    }

    private boolean hasSubdirectories(File dir) {
        File[] children = dir.listFiles(File::isDirectory);
        return children != null && children.length > 0;
    }

    private void markDone() {
        try {
            File parent = MARKER_FILE.getParentFile();
            if (!parent.exists()) Files.createDirectories(parent.toPath());
            Files.writeString(MARKER_FILE.toPath(), Instant.now().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not persist the first-time setup marker.", e);
        }
    }

    public record SetupStatus(boolean needsSetup, boolean needsAdminAccount) {}
}
