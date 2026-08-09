package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.CloudService;
import dev.simstoe.lupocloud.api.models.ServiceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ICloudManager cloudManager;

    public ServiceController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    public record StartServiceRequest(String name, ServiceType type, int port, String templateName) {}

    public record CommandRequest(String command) {}

    @GetMapping
    public List<CloudService> list() {
        return cloudManager.activeServices();
    }

    @GetMapping("/{name}")
    public ResponseEntity<CloudService> get(@PathVariable String name) {
        return cloudManager.service(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> start(@RequestBody StartServiceRequest request) {
        if (request.type() == ServiceType.VELOCITY) {
            cloudManager.startProxy(request.name(), request.port(), request.templateName());
        } else {
            cloudManager.startServer(request.name(), request.port(), request.templateName());
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{name}/stop")
    public ResponseEntity<Void> stop(@PathVariable String name) {
        cloudManager.stopService(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stop-all")
    public ResponseEntity<Void> stopAll() {
        cloudManager.stopAll();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{name}/command")
    public ResponseEntity<Void> sendCommand(@PathVariable String name, @RequestBody CommandRequest request) {
        cloudManager.sendCommand(name, request.command());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{name}/logs")
    public List<String> logs(@PathVariable String name) {
        return cloudManager.serviceLogs(name);
    }
}
