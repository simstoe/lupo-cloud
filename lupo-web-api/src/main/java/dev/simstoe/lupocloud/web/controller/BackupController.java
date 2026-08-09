package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services/{name}/backups")
public class BackupController {
    private final ICloudManager cloudManager;

    public BackupController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @GetMapping
    public List<String> list(@PathVariable String name) {
        return cloudManager.backups(name);
    }

    @PostMapping
    public ResponseEntity<Void> create(@PathVariable String name) {
        cloudManager.createBackup(name);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{file}/restore")
    public ResponseEntity<Void> restore(@PathVariable String name, @PathVariable String file) {
        cloudManager.restoreBackup(name, file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{file}")
    public ResponseEntity<Void> delete(@PathVariable String name, @PathVariable String file) {
        cloudManager.deleteBackup(name, file);
        return ResponseEntity.noContent().build();
    }
}
