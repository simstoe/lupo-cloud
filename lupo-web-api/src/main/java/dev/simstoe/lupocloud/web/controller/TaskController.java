package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final ICloudManager cloudManager;

    public TaskController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @GetMapping
    public List<ServiceTask> list() {
        return cloudManager.tasks().stream()
                .map(cloudManager::task)
                .flatMap(Optional::stream)
                .toList();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ServiceTask> get(@PathVariable String name) {
        return cloudManager.task(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody ServiceTask task) {
        cloudManager.createTask(task);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{name}")
    public ResponseEntity<Void> update(@PathVariable String name, @RequestBody ServiceTask task) {
        cloudManager.saveTask(task);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        cloudManager.deleteTask(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{name}/start")
    public ResponseEntity<Void> start(@PathVariable String name) {
        cloudManager.startFromTask(name);
        return ResponseEntity.accepted().build();
    }
}
