package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.MonitoringSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    private final ICloudManager cloudManager;

    public MonitoringController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @GetMapping
    public MonitoringSnapshot get() {
        return cloudManager.monitoring();
    }
}
