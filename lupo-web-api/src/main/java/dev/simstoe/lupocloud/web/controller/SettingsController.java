package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.MemoryBudget;
import dev.simstoe.lupocloud.api.models.NetworkSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final ICloudManager cloudManager;

    public SettingsController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @GetMapping
    public NetworkSettings get() {
        return cloudManager.settings();
    }

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody NetworkSettings settings) {
        cloudManager.updateSettings(settings);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/memory")
    public MemoryBudget memory() {
        return cloudManager.memoryBudget();
    }

    @PostMapping("/danger/delete-all-templates")
    public ResponseEntity<Void> deleteAllTemplates() {
        cloudManager.deleteAllTemplates();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/danger/reset-cloud")
    public ResponseEntity<Void> resetCloud() {
        cloudManager.resetCloud();
        return ResponseEntity.noContent().build();
    }
}
